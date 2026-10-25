package com.disheveled.bills.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private fun reply(content: String?, finish: String = "stop") =
    ChatResponse(choices = listOf(Choice(message = ResponseMessage(content = content), finishReason = finish)))

class NoteParsingTest {

    @Test
    fun readsTheNoteField() {
        assertEquals(
            "Juple bayar Rp 21.500, Peter Rp 22.000.",
            noteFrom(reply("""{"note": " Juple bayar Rp 21.500, Peter Rp 22.000. "}""")),
        )
    }

    @Test
    fun thinkingThatLeaksBeforeTheJsonIsDropped() {
        assertEquals("Sama rata.", noteFrom(reply("""mikir dulu…</think>{"note": "Sama rata."}""")))
    }

    @Test
    fun freeTextIsNeverShown() {
        // What reached the screen before the schema: the model still thinking, in content.
        val leaked = "? Actually format \"Rp 21.500\". Use that.\n\nThus final answer: \"Juple dan Peter " +
            "membayar sedikit berbeda.\"\n\nJuple dan Peter membayar sedikit berbeda."
        assertNull(noteFrom(reply(leaked)))
    }

    @Test
    fun aCutOffOrEmptyReplyIsNoNote() {
        assertNull(noteFrom(reply("""{"note": "Juple bayar""", finish = "length")))
        assertNull(noteFrom(reply("""{"note": "  "}""")))
        assertNull(noteFrom(reply(null)))
        assertNull(noteFrom(ChatResponse()))
    }
}
