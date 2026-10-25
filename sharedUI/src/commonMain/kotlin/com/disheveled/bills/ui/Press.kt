package com.disheveled.bills.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** The system's "remove animations" setting, provided by the platform shell. */
internal val LocalReducedMotion = staticCompositionLocalOf { false }

/** How a control answers a press. Nota has no ripple anywhere. */
internal enum class Press(internal val shrinkTo: Float) {
    /** Buttons, chips, tabs, icon buttons: shrink to 96%. */
    Small(0.96f),

    /** Cards that open something: shrink only to 98%, a big surface shouldn't lurch. */
    Large(0.98f),

    /** Rows inside a receipt: turn paper-sunk instead, so they don't shift inside the paper. */
    Row(1f),
}

// Nota's motion: in fast and eased out, back out with a slight overshoot so it feels sprung.
private val PressIn = tween<Float>(90, easing = CubicBezierEasing(0f, 0f, 0.58f, 1f))
private val PressOut = tween<Float>(220, easing = CubicBezierEasing(0.3f, 1.4f, 0.5f, 1f))

/**
 * A click with Nota's press feedback instead of a ripple. Put it first in the modifier chain:
 * the scale only reaches what is drawn after it, so a background set earlier wouldn't shrink.
 */
@Composable
internal fun Modifier.pressable(
    onClick: () -> Unit,
    enabled: Boolean = true,
    press: Press = Press.Small,
    role: Role? = Role.Button,
    onClickLabel: String? = null,
): Modifier = clickable(
    // No source of our own: clickable makes one, and the feedback node, on the first touch.
    interactionSource = null,
    indication = notaPress(press),
    enabled = enabled,
    onClickLabel = onClickLabel,
    role = role,
    onClick = onClick,
)

/**
 * Just the feedback, for when the press is caught elsewhere: the bottom nav shrinks its pill
 * while the whole tab takes the touch.
 */
@Composable
internal fun Modifier.pressFeedback(interaction: InteractionSource, press: Press = Press.Small): Modifier =
    indication(interaction, notaPress(press))

@Composable
private fun notaPress(press: Press) = NotaPress(
    press = press,
    sunk = MaterialTheme.colorScheme.surfaceVariant,
    reduced = LocalReducedMotion.current,
)

/**
 * The feedback as an indication rather than composed state: every tappable used to compose an
 * interaction source and three animations up front, and recomposed its caller on each press.
 * The node exists only once something is touched and animates in the draw phase.
 */
private data class NotaPress(val press: Press, val sunk: Color, val reduced: Boolean) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        PressNode(interactionSource, press, sunk, reduced)
}

private class PressNode(
    private val interactions: InteractionSource,
    private val press: Press,
    private val sunk: Color,
    private val reduced: Boolean,
) : Modifier.Node(), DrawModifierNode {
    /** 0 at rest, 1 held; dips below 0 on the way out with PressOut's overshoot. */
    private val held = Animatable(0f)
    private val dim = Paint()

    override fun onAttach() {
        coroutineScope.launch {
            val down = mutableListOf<PressInteraction.Press>()
            interactions.interactions.collectLatest { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> down += interaction
                    is PressInteraction.Release -> down -= interaction.press
                    is PressInteraction.Cancel -> down -= interaction.press
                    else -> return@collectLatest
                }
                if (down.isNotEmpty()) held.animateTo(1f, PressIn) else held.animateTo(0f, PressOut)
            }
        }
    }

    override fun ContentDrawScope.draw() {
        val t = held.value
        if (t == 0f) return drawContent()
        val shown = t.coerceIn(0f, 1f)
        when {
            press == Press.Row -> {
                drawRoundRect(sunk.copy(alpha = sunk.alpha * shown), cornerRadius = CornerRadius(8.dp.toPx()))
                drawContent()
            }
            // "Remove animations": dim to 80% instead of moving.
            reduced -> {
                dim.alpha = 1f - 0.2f * shown
                drawContext.canvas.saveLayer(size.toRect(), dim)
                drawContent()
                drawContext.canvas.restore()
            }
            else -> scale(1f - (1f - press.shrinkTo) * t) { this@draw.drawContent() }
        }
    }
}
