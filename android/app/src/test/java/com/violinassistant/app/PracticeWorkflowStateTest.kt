package com.violinassistant.app

import com.violinassistant.app.model.NoteEvent
import com.violinassistant.app.model.NoteStatus
import com.violinassistant.app.model.PitchResult
import com.violinassistant.app.practice.EngineStatus
import com.violinassistant.app.practice.PracticeEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Requirement R2 & R3: Practice Workflow, State Transitions, and Cross-Feature Combinations.
 * Covers Tier 1 (Start/Pause/Restart) and Tier 3 (Cross-feature interplay).
 */
class PracticeWorkflowStateTest {
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
            )
        )
        engine = PracticeEngine(notes, tolerancePercent = 20)
    }

    private fun mockPitch(midi: Int, cents: Int = 0): PitchResult =
        PitchResult(frequency = 440f, midi = midi, pitchName = "M$midi", centsDeviation = cents, clarity = 0.95f, volume = 0.5f, isStable = true)

    // Tier 1: Initial state & reset state transitions
    @Test
    fun testInitialAndResetStateTransitions() {
        val initialState = engine.getState()
        assertEquals(0, initialState.currentIndex)
        assertEquals(EngineStatus.WAITING_FOR_NOTE, initialState.status)
        assertEquals(0, initialState.totalWrongAttempts)
        assertEquals(NoteStatus.ACTIVE, initialState.noteStatuses[0])
        assertNull(initialState.overallScore)
        assertEquals("n1", initialState.currentExpectedNote?.id)

        // Simulate some practice progress
        engine.processAudioInput(mockPitch(69), timestampMs = 1000L)
        engine.processAudioInput(mockPitch(69), timestampMs = 1850L)
        assertEquals(1, engine.getState().currentIndex)

        // Now trigger reset (Restart action)
        engine.reset()
        val resetState = engine.getState()
        assertEquals(0, resetState.currentIndex)
        assertEquals(EngineStatus.WAITING_FOR_NOTE, resetState.status)
        assertEquals(0, resetState.totalWrongAttempts)
        assertEquals(0, resetState.evaluations.size)
        assertEquals(NoteStatus.ACTIVE, resetState.noteStatuses[0])
        assertNull(resetState.overallScore)
        assertEquals("n1", resetState.currentExpectedNote?.id)
    }

    // Tier 3: Pitch error followed by correct pitch with wrong timing
    @Test
    fun testPitchErrorFollowedByCorrectPitchWithWrongTiming() {
        var t = 1000L

        // 1. Play wrong pitch G4 (67) instead of A4 (69)
        engine.processAudioInput(mockPitch(67), timestampMs = t)
        val wrongPitchState = engine.getState()
        assertEquals(0, wrongPitchState.currentIndex)
        assertEquals(1, wrongPitchState.totalWrongAttempts)
        assertEquals(NoteStatus.WRONG_PITCH, wrongPitchState.noteStatuses[0])

        // 2. Play another wrong pitch F4 (65)
        t += 100L
        engine.processAudioInput(mockPitch(65), timestampMs = t)
        assertEquals(2, engine.getState().totalWrongAttempts)
        assertEquals(0, engine.getState().currentIndex)

        // 3. User now plays correct pitch A4 (69), but only for 200ms on 1.0s note
        t += 200L
        engine.processAudioInput(mockPitch(69), timestampMs = t)
        t += 200L
        engine.processAudioInput(null, timestampMs = t) // released early

        val finalState = engine.getState()
        // Cursor must advance to next note (n2)
        assertEquals("Cursor must advance after correct pitch played >20ms", 1, finalState.currentIndex)
        assertEquals(NoteStatus.WRONG_DURATION, finalState.noteStatuses[0])
        assertEquals("n2", finalState.currentExpectedNote?.id)

        // Verify evaluation record integrity
        assertEquals(1, finalState.evaluations.size)
        val eval = finalState.evaluations[0]
        assertTrue(eval.pitchCorrect)
        assertFalse(eval.durationCorrect)
        assertEquals(2, eval.wrongAttemptsCount)
        assertEquals(2, finalState.totalWrongAttempts)
    }

    // Tier 3: Pause/Silence during note sustain and clean resume
    @Test
    fun testPauseDuringNoteSustainAndResume() {
        val baseTime = 5000L
        // Start sustaining correct pitch
        engine.processAudioInput(mockPitch(69), timestampMs = baseTime)
        assertEquals(EngineStatus.MEASURING_DURATION, engine.getState().status)

        // Note drops to silence after 10ms (pause or stutter < 20ms transient)
        engine.processAudioInput(null, timestampMs = baseTime + 10L)
        val pausedState = engine.getState()
        assertEquals(0, pausedState.currentIndex)
        assertEquals(EngineStatus.WAITING_FOR_NOTE, pausedState.status)
        assertEquals(0, pausedState.evaluations.size)

        // Resume: user starts again at baseTime + 500ms and sustains full 850ms
        val resumeTime = baseTime + 500L
        engine.processAudioInput(mockPitch(69), timestampMs = resumeTime)
        engine.processAudioInput(mockPitch(69), timestampMs = resumeTime + 850L)

        val resumedState = engine.getState()
        assertEquals(1, resumedState.currentIndex)
        assertEquals(NoteStatus.CORRECT, resumedState.noteStatuses[0])
        assertEquals(1, resumedState.evaluations.size)
        assertTrue(resumedState.evaluations[0].durationCorrect)
    }

    // Tier 3: Score and evaluation state reset on restart after mid-session completion
    @Test
    fun testScoreAndEvaluationsResetOnRestart() {
        // Complete note 1 and note 2 with some wrong attempts
        var t = 1000L
        engine.processAudioInput(mockPitch(60), timestampMs = t) // wrong
        t += 100L
        engine.processAudioInput(mockPitch(69), timestampMs = t) // note 1 correct
        t += 850L
        engine.processAudioInput(mockPitch(69), timestampMs = t)
        assertEquals(1, engine.getState().currentIndex)

        engine.processAudioInput(mockPitch(71), timestampMs = t + 50L) // note 2 correct
        t += 900L
        engine.processAudioInput(mockPitch(71), timestampMs = t)
        assertEquals(2, engine.getState().currentIndex)
        assertEquals(2, engine.getState().evaluations.size)
        assertEquals(1, engine.getState().totalWrongAttempts)

        // Restart
        engine.reset()
        val resetState = engine.getState()
        assertEquals(0, resetState.currentIndex)
        assertEquals(0, resetState.evaluations.size)
        assertEquals(0, resetState.totalWrongAttempts)
        assertNull(resetState.overallScore)
        assertEquals("n1", resetState.currentExpectedNote?.id)
        assertEquals(NoteStatus.ACTIVE, resetState.noteStatuses[0])
        assertNull(resetState.noteStatuses[1])
    }
}
