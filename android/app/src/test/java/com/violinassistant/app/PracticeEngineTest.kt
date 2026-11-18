package com.violinassistant.app

import com.violinassistant.app.model.NoteEvent
import com.violinassistant.app.model.NoteStatus
import com.violinassistant.app.model.PitchResult
import com.violinassistant.app.practice.PracticeEngine
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PracticeEngineTest {
    private lateinit var notes: List<NoteEvent>
    private lateinit var engine: PracticeEngine

    @Before
    fun setup() {
        notes = listOf(
            NoteEvent(
                id = "n1", step = "C", alter = 0, octave = 4, midi = 60,
                pitchName = "C4", durationInBeats = 1f, expectedDurationSec = 0.8f,
                measure = 1, voice = 1, isRest = false
            ),
            NoteEvent(
                id = "n2", step = "D", alter = 0, octave = 4, midi = 62,
                pitchName = "D4", durationInBeats = 1f, expectedDurationSec = 0.8f,
                measure = 1, voice = 1, isRest = false
            ),
            NoteEvent(
                id = "n3", step = "E", alter = 0, octave = 4, midi = 64,
                pitchName = "E4", durationInBeats = 1f, expectedDurationSec = 0.8f,
                measure = 1, voice = 1, isRest = false
            )
        )
        engine = PracticeEngine(notes, tolerancePercent = 20)
    }

    private fun mockPitch(midi: Int, isStable: Boolean = true): PitchResult =
        PitchResult(440f, midi, "P$midi", 0, 0.95f, 0.1f, isStable)

    @Test
    fun testCaseAWrongPitchDoesNotAdvance() {
        // Expected is C4 (60). User plays D4 (62).
        engine.processAudioInput(mockPitch(62))
        val state = engine.getState()
        assertEquals(0, state.currentIndex) // MUST REMAIN AT INDEX 0
        assertEquals(NoteStatus.WRONG_PITCH, state.noteStatuses[0])
        assertEquals(1, state.totalWrongAttempts)
    }

    @Test
    fun testCaseDSilenceDoesNotAdvance() {
        engine.processAudioInput(null)
        assertEquals(0, engine.getState().currentIndex)

        engine.processAudioInput(mockPitch(60, isStable = false))
        assertEquals(0, engine.getState().currentIndex)
    }

    @Test
    fun testCaseEFutureNoteCannotBeSkipped() {
        // Hearing E4 (64) while waiting for C4 (60)
        engine.processAudioInput(mockPitch(64))
        val state = engine.getState()
        assertEquals(0, state.currentIndex)
        assertEquals("n1", state.currentExpectedNote?.id)
    }

    @Test
    fun testCaseBWrongThenCorrectPitchAdvances() {
        // 1. Wrong attempt
        engine.processAudioInput(mockPitch(62))
        assertEquals(0, engine.getState().currentIndex)

        // 2. Play correct note
        engine.processAudioInput(mockPitch(60))
        assertEquals(0, engine.getState().currentIndex)

        // 3. Release note
        engine.processAudioInput(null)
        assertEquals(1, engine.getState().currentIndex)
    }
}
