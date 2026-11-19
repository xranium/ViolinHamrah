package com.violinassistant.app

import com.violinassistant.app.model.NoteEvent
import com.violinassistant.app.model.NoteStatus
import com.violinassistant.app.model.PitchResult
import com.violinassistant.app.practice.EngineStatus
import com.violinassistant.app.practice.PracticeEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Requirement R3 & R4: Practice Engine Validation Test Suite.
 * Covers Tier 1 (Feature Coverage) and Tier 2 (Boundary & Corner Cases).
 */
class PracticeEngineValidationTest {
    private lateinit var notes: List<NoteEvent>
    private lateinit var engine: PracticeEngine

    @Before
    fun setup() {
        notes = listOf(
            NoteEvent(
                id = "n1", step = "A", alter = 0, octave = 4, midi = 69,
                pitchName = "A4", durationInBeats = 1f, expectedDurationSec = 1.0f,
                measure = 1, voice = 1, isRest = false
            ),
            NoteEvent(
                id = "n2", step = "B", alter = 0, octave = 4, midi = 71,
                pitchName = "B4", durationInBeats = 1f, expectedDurationSec = 1.0f,
                measure = 1, voice = 1, isRest = false
            ),
            NoteEvent(
                id = "n3", step = "C", alter = 1, octave = 5, midi = 73,
                pitchName = "C#5", durationInBeats = 1f, expectedDurationSec = 1.0f,
                measure = 1, voice = 1, isRest = false
            ),
            NoteEvent(
                id = "r1", step = "R", alter = 0, octave = 0, midi = 0,
                pitchName = "سکوت", durationInBeats = 1f, expectedDurationSec = 0.5f,
                measure = 2, voice = 1, isRest = true
            )
        )
        engine = PracticeEngine(notes, tolerancePercent = 20)
    }

    private fun mockPitch(midi: Int, cents: Int = 0, isStable: Boolean = true): PitchResult =
        PitchResult(frequency = 440f, midi = midi, pitchName = "M$midi", centsDeviation = cents, clarity = 0.95f, volume = 0.5f, isStable = isStable)

    // Tier 1: Expected Note Display & Initial Cursor
    @Test
    fun testExpectedNoteDisplayAtStart() {
        val state = engine.getState()
        assertEquals(0, state.currentIndex)
        assertEquals("n1", state.currentExpectedNote?.id)
        assertEquals(69, state.currentExpectedNote?.midi)
        assertEquals("A4", state.currentExpectedNote?.pitchName)
        assertEquals(NoteStatus.ACTIVE, state.noteStatuses[0])
    }

    // Tier 1: Cursor immobility on wrong pitch
    @Test
    fun testWrongPitchDoesNotAdvanceCursor() {
        engine.processAudioInput(mockPitch(60), timestampMs = 1000L) // Played C4 instead of A4 (69)
        val state = engine.getState()
        assertEquals(0, state.currentIndex)
        assertEquals(NoteStatus.WRONG_PITCH, state.noteStatuses[0])
        assertEquals(1, state.totalWrongAttempts)
        assertEquals(EngineStatus.WAITING_FOR_NOTE, state.status)
    }

    // Tier 1: Cursor advancement on accepted note
    @Test
    fun testCursorAdvancesOnAcceptedNote() {
        val baseTime = 1000L
        engine.processAudioInput(mockPitch(69), timestampMs = baseTime)
        // Sustain note for 800ms (minAcceptable for 1.0s note at 20% tolerance = 0.8s)
        engine.processAudioInput(mockPitch(69), timestampMs = baseTime + 850L)

        val state = engine.getState()
        assertEquals(1, state.currentIndex)
        assertEquals("n2", state.currentExpectedNote?.id)
        assertEquals(NoteStatus.CORRECT, state.noteStatuses[0])
    }

    // Tier 2: Boundary - 1ms transient noise rejection
    @Test
    fun testOneMillisecondTransientNoiseRejection() {
        val baseTime = 2000L
        engine.processAudioInput(mockPitch(69), timestampMs = baseTime)
        // Silence after only 1ms
        engine.processAudioInput(null, timestampMs = baseTime + 1L)

        val state = engine.getState()
        assertEquals("1ms noise must not advance cursor", 0, state.currentIndex)
        assertEquals(EngineStatus.WAITING_FOR_NOTE, state.status)
        assertEquals(0, state.evaluations.size)
    }

    // Tier 2: Boundary - Exactly 20ms boundary (sustained >= 20ms is accepted)
    @Test
    fun testTwentyMillisecondTransientBoundary() {
        val baseTime = 3000L
        engine.processAudioInput(mockPitch(69), timestampMs = baseTime)
        // 19ms transient -> rejected
        engine.processAudioInput(null, timestampMs = baseTime + 19L)
        assertEquals("19ms transient must be rejected", 0, engine.getState().currentIndex)

        // Now test 20ms sustain -> accepted as played note (even if short duration)
        val secondTime = 4000L
        engine.processAudioInput(mockPitch(69), timestampMs = secondTime)
        engine.processAudioInput(null, timestampMs = secondTime + 20L)

        val state = engine.getState()
        assertEquals("20ms sustain reaches transient threshold and advances note", 1, state.currentIndex)
        assertEquals(1, state.evaluations.size)
    }

    // Tier 2: Corner Case - Rapid consecutive notes (each sustained >= 20ms)
    @Test
    fun testRapidConsecutiveNotes() {
        var t = 5000L
        val midis = listOf(69, 71, 73)

        for (midi in midis) {
            engine.processAudioInput(mockPitch(midi), timestampMs = t)
            t += 30L // 30ms sustain (> 20ms threshold)
            engine.processAudioInput(null, timestampMs = t)
            t += 10L
        }

        // Rest note r1 advances automatically on processAudioInput
        engine.processAudioInput(null, timestampMs = t)

        val state = engine.getState()
        assertEquals("All notes including rest should be completed", 4, state.currentIndex)
        assertEquals(EngineStatus.FINISHED, state.status)
    }

    // Rest note auto-advance
    @Test
    fun testRestNoteAutoAdvances() {
        // Complete notes 0, 1, 2
        var t = 1000L
        listOf(69, 71, 73).forEach { midi ->
            engine.processAudioInput(mockPitch(midi), timestampMs = t)
            t += 850L
            engine.processAudioInput(mockPitch(midi), timestampMs = t)
        }
        assertEquals(3, engine.getState().currentIndex)
        assertEquals("r1", engine.getState().currentExpectedNote?.id)

        // Rest note advances automatically upon any input
        engine.processAudioInput(null, timestampMs = t)
        assertEquals(4, engine.getState().currentIndex)
        assertEquals(EngineStatus.FINISHED, engine.getState().status)
    }
}
