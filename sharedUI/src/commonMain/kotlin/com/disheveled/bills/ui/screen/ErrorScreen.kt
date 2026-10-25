package com.disheveled.bills.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.disheveled.bills.ai.FailureReason
import com.disheveled.bills.ui.NotaGlyph
import com.disheveled.bills.ui.Glyph
import com.disheveled.bills.ui.PrimaryButton
import com.disheveled.bills.ui.ScreenTopBar
import com.disheveled.bills.ui.bottomBarInsets
import com.disheveled.bills.ui.SecondaryButton
import com.disheveled.bills.ui.SurfaceCard

@Composable
internal fun ErrorScreen(
    reason: FailureReason,
    detail: String?,
    onRetry: () -> Unit,
    onManualEntry: () -> Unit,
    onBack: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val missingKey = reason == FailureReason.MissingApiKey

    Column(Modifier.fillMaxSize().background(scheme.background)) {
        ScreenTopBar(title = "", onBack = onBack)

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (missingKey) KeyMark() else TornReceiptMark()

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = title(reason),
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 27.sp, lineHeight = 32.sp),
                    color = scheme.onBackground,
                )
                Text(
                    text = body(reason),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            }

            if (missingKey) ApiKeySteps() else TipsCard(reason)

            if (detail != null && !missingKey) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            if (missingKey) {
                Text(
                    "Key-nya cuma dipakai buat manggil NVIDIA NIM langsung dari HP kamu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }

        Column(
            modifier = Modifier.bottomBarInsets()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (missingKey) {
                // Retrying without a key just fails again, so manual entry leads here.
                PrimaryButton(label = "Input manual", onClick = onManualEntry)
                SecondaryButton(label = "Udah diisi, coba lagi", onClick = onRetry)
            } else {
                PrimaryButton(
                    label = "Foto ulang",
                    icon = { Glyph(NotaGlyph.Camera, scheme.onPrimary) },
                    onClick = onRetry,
                )
                SecondaryButton(label = "Input manual", onClick = onManualEntry)
            }
        }
    }
}

private fun title(reason: FailureReason) = when (reason) {
    FailureReason.MissingApiKey -> "AI-nya belum nyala"
    FailureReason.Network -> "Koneksi lagi ngambek"
    FailureReason.RateLimited -> "Kebanyakan scan barusan"
    FailureReason.ApiError -> "Ada yang error di AI-nya"
    FailureReason.Refused -> "AI-nya nggak mau baca ini"
    FailureReason.Truncated -> "Struknya kepanjangan"
    FailureReason.Unreadable -> "Struknya nggak kebaca"
}

private fun body(reason: FailureReason) = when (reason) {
    FailureReason.MissingApiKey ->
        "Bills butuh API key buat baca struk. Isi sekali di local.properties, habis itu tinggal scan."
    FailureReason.Network ->
        "Nggak bisa nyambung ke server. Cek koneksi kamu terus coba lagi."
    FailureReason.RateLimited ->
        "Limit-nya kena. Tunggu sebentar, terus coba scan lagi."
    FailureReason.ApiError ->
        "Server-nya balikin error. Coba lagi, atau input manual dulu kalau buru-buru."
    FailureReason.Refused ->
        "Fotonya ditolak buat dibaca. Coba foto struknya aja tanpa hal lain di frame."
    FailureReason.Truncated ->
        "Item-nya kebanyakan buat sekali baca. Coba foto per bagian, atau input manual."
    FailureReason.Unreadable ->
        "Fotonya kurang jelas, jadi item-nya nggak ketemu. Coba foto ulang atau masukin manual aja."
}

@Composable
private fun ApiKeySteps() {
    val scheme = MaterialTheme.colorScheme
    SurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            NumberedStep(1, "Ambil key gratis di aistudio.google.com → Get API key")
            NumberedStep(2, "Tambahin barisnya di local.properties")
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
                    .background(scheme.surfaceVariant).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("nama field", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                Text(
                    "GEMINI_API_KEY = AIza...",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp, letterSpacing = 0.sp),
                    color = scheme.onSurface,
                )
            }
            NumberedStep(3, "Build ulang app-nya, terus scan lagi")
        }
    }
}

@Composable
private fun NumberedStep(number: Int, text: String) {
    val scheme = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier.size(22.dp).clip(CircleShape).background(scheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "$number",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                color = scheme.primary,
            )
        }
        Text(text, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp), color = scheme.onSurface)
    }
}

@Composable
private fun TipsCard(reason: FailureReason) {
    val scheme = MaterialTheme.colorScheme
    val tips = when (reason) {
        FailureReason.Network, FailureReason.RateLimited, FailureReason.ApiError -> listOf(
            "Pastiin HP kamu online",
            "Tunggu beberapa detik sebelum coba lagi",
            "Kalau mendesak, input manual aja dulu",
        )
        else -> listOf(
            "Taruh struk di permukaan datar",
            "Pastiin semua item keliatan, jangan kepotong",
            "Hindari bayangan atau kilatan lampu",
        )
    }
    SurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                if (reason == FailureReason.Unreadable) "Biar kebaca:" else "Coba ini:",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                color = scheme.onSurface,
            )
            tips.forEach { tip ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(scheme.primary))
                    Text(tip, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp), color = scheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun KeyMark() {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(scheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(26.dp).drawBehind {
                val stroke = 1.75.dp.toPx()
                val bodyTop = size.height * 0.45f
                // Shackle
                drawArc(
                    color = scheme.primary,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.25f, bodyTop - size.height * 0.32f),
                    size = Size(size.width * 0.5f, size.height * 0.5f),
                    style = Stroke(width = stroke),
                )
                // Body
                drawRoundRect(
                    color = scheme.primary,
                    topLeft = Offset(size.width * 0.12f, bodyTop),
                    size = Size(size.width * 0.76f, size.height * 0.5f),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                    style = Stroke(width = stroke),
                )
            },
        )
    }
}

/** Faded receipt with a slash through it — the canvas's "couldn't read this" mark. */
@Composable
private fun TornReceiptMark() {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.size(width = 150.dp, height = 190.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(scheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                listOf(0.70f, 1f, 0.45f, 0.85f, 0.60f).forEach { width ->
                    Box(
                        Modifier.fillMaxWidth(width).height(6.dp)
                            .clip(RoundedCornerShape(2.dp)).background(scheme.outlineVariant),
                    )
                }
            }
            Box(
                Modifier.fillMaxWidth().height(2.dp).rotate(-14f).background(scheme.error),
            )
        }
    }
}
