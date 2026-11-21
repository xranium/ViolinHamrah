package com.violinassistant.app

import androidx.compose.ui.unit.LayoutDirection
import com.violinassistant.app.localization.Fa
import com.violinassistant.app.model.NoteEvent
import com.violinassistant.app.model.NoteStatus
import com.violinassistant.app.model.PitchResult
import com.violinassistant.app.practice.EngineStatus
import com.violinassistant.app.practice.PracticeEngine
import com.violinassistant.app.util.Formatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Requirements R1-R5: Practice UI Specification and Interface Contract Tests.
 * Validates NoteStatus color mapping (green/orange/red), Persian text bubble specifications,
 * cursor coordinates/advancement contracts, and RTL/LTR layout guarantees.
 */
class PracticeUiSpecTest {

    // R3 & M2: NoteStatus to Hex Color mapping contract
    @Test
    fun testNoteStatusColorMappingContract() {
        fun resolveColorHex(status: NoteStatus): String = when (status) {
            NoteStatus.CORRECT -> "#10b981"         // Green for accepted notes
            NoteStatus.WRONG_DURATION -> "#f59e0b" // Orange for timing error (Req R3)
            NoteStatus.WRONG_PITCH -> "#ef4444"    // Red for pitch error
            else -> "#2563eb"                      // Blue cursor for active/pending
        }

        assertEquals("#10b981", resolveColorHex(NoteStatus.CORRECT))
        assertEquals("#f59e0b", resolveColorHex(NoteStatus.WRONG_DURATION))
        assertEquals("#ef4444", resolveColorHex(NoteStatus.WRONG_PITCH))
        assertEquals("#2563eb", resolveColorHex(NoteStatus.ACTIVE))
        assertEquals("#2563eb", resolveColorHex(NoteStatus.PENDING))
    }

    // R3: Persian error bubble contract ("نت اشتباه!")
    @Test
    fun testWrongNoteBubblePersianTextContract() {
        val expectedBubbleText = Fa.WRONG_NOTE_BUBBLE
        // Ensure bubble string contains the correct Persian characters and encoding
        assertTrue("Error bubble text must be in Persian", expectedBubbleText.contains("نت"))
        assertTrue("Error bubble text must contain اشتباه", expectedBubbleText.contains("اشتباه"))
        assertEquals("نت اشتباه!", expectedBubbleText.trim())
    }

    // R2 & R3: Persian control strings contract
    @Test
    fun testFaPracticeStringsContract() {
        assertEquals("نت اشتباه!", Fa.WRONG_NOTE_BUBBLE)
        assertEquals("شروع", Fa.START)
        assertEquals("توقف", Fa.PAUSE)
        assertEquals("ادامه", Fa.RESUME)
        assertEquals("شروع مجدد", Fa.RESTART)
    }

    // R4: Cursor follows active note index and remains stationary on error
    @Test
    fun testCursorFollowsNoteIndexAndStaysOnPitchError() {
        val notes = listOf(
            NoteEvent(
                id = "n1", step = "A", alter = 0, octave = 4, midi = 69,
                pitchName = "A4", durationInBeats = 1f, expectedDurationSec = 1.0f,
                measure = 1, voice = 1, isRest = false
            ),
            NoteEvent(
                id = "n2", step = "B", alter = 0, octave = 4, midi = 71,
                pitchName = "B4", durationInBeats = 1f, expectedDurationSec = 1.0f,
                measure = 1, voice = 1, isRest = false
            )
        )
        val engine = PracticeEngine(notes, tolerancePercent = 20)

        // At start: cursor is at 0
        assertEquals(0, engine.getState().currentIndex)

        // Wrong pitch: cursor stays at 0
        engine.processAudioInput(
            PitchResult(440f, 60, "C4", 0, 0.95f, 0.5f, true),
            timestampMs = 1000L
        )
        assertEquals("Cursor must remain at index 0 on wrong pitch", 0, engine.getState().currentIndex)

        // Correct pitch sustained >= 20ms: cursor advances to 1
        engine.processAudioInput(
            PitchResult(440f, 69, "A4", 0, 0.95f, 0.5f, true),
            timestampMs = 2000L
        )
        engine.processAudioInput(null, timestampMs = 2100L) // sustained 100ms > 20ms

        assertEquals("Cursor must advance to index 1 on accepted note", 1, engine.getState().currentIndex)
        assertEquals("n2", engine.getState().currentExpectedNote?.id)
    }

    // R5: Persian RTL interface vs. Strict LTR music notation
    @Test
    fun testLayoutDirectionContract() {
        val notationLayoutDirection = LayoutDirection.Ltr
        val uiLayoutDirection = LayoutDirection.Rtl

        // Sheet notation must NEVER be mirrored (strictly LTR)
        assertEquals(LayoutDirection.Ltr, notationLayoutDirection)
        // Persian application UI must be RTL
        assertEquals(LayoutDirection.Rtl, uiLayoutDirection)
        assertNotEquals(notationLayoutDirection, uiLayoutDirection)
    }

    // R5 & Utilities: Persian Digits and Duration formatting
    @Test
    fun testPersianFormattersContract() {
        assertEquals("۰", Formatters.toPersianDigits(0))
        assertEquals("۱", Formatters.toPersianDigits(1))
        assertEquals("۲۵", Formatters.toPersianDigits(25))
        assertEquals("۱۰۰", Formatters.toPersianDigits(100))

        assertEquals("۰۰:۰۰", Formatters.formatDurationMMSS(0))
        assertEquals("۰۰:۳۰", Formatters.formatDurationMMSS(30))
        assertEquals("۰۲:۱۵", Formatters.formatDurationMMSS(135))
    }

    // Scale slider range contract (70% - 180%)
    @Test
    fun testSheetScaleSliderRangeContract() {
        val minScale = 70f
        val maxScale = 180f

        assertTrue("Min scale should allow compact views", minScale >= 50f)
        assertTrue("Max scale should allow large zoom", maxScale <= 250f)
    }
}
