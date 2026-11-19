package com.violinassistant.app

import com.violinassistant.app.model.NoteEvent
import com.violinassistant.app.model.NoteStatus
import com.violinassistant.app.model.PitchResult
import com.violinassistant.app.musicxml.MusicXmlParser
import com.violinassistant.app.musicxml.sample.SAMPLE_D_MAJOR_SCALE_XML
import com.violinassistant.app.practice.EngineStatus
import com.violinassistant.app.practice.EvaluationRecord
import com.violinassistant.app.practice.PracticeEngine
import com.violinassistant.app.practice.ScoringEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Requirement R3 & Scoring Engine: Scoring Integrity and Evaluation Test Suite.
 * Covers Tier 1, Tier 2 (Boundary: 200ms on 1s, extreme cents, zero duration), and Tier 4 (Real-World Full Score).
 */
class ScoringIntegrityTest {

    private fun mockPitch(midi: Int, cents: Int = 0): PitchResult =
        PitchResult(frequency = 440f, midi = midi, pitchName = "M$midi", centsDeviation = cents, clarity = 0.95f, volume = 0.5f, isStable = true)

    // Tier 1 & 2: Note duration 200ms on 1.0s note yields WRONG_DURATION and penalizes score
    @Test
    fun testTwoHundredMillisecOnOneSecNoteYieldsWrongDurationAndDegradesScore() {
        val note = NoteEvent(
            id = "n1", step = "A", alter = 0, octave = 4, midi = 69,
            pitchName = "A4", durationInBeats = 1f, expectedDurationSec = 1.0f,
            measure = 1, voice = 1, isRest = false
        )
        val engine = PracticeEngine(listOf(note), tolerancePercent = 20)

        val t0 = 1000L
        engine.processAudioInput(mockPitch(69), timestampMs = t0)
        // End note after 200ms (0.2s played on 1.0s expected note)
        engine.processAudioInput(null, timestampMs = t0 + 200L)

        val state = engine.getState()
        assertEquals(1, state.currentIndex)
        assertEquals(EngineStatus.FINISHED, state.status)
        assertEquals(NoteStatus.WRONG_DURATION, state.noteStatuses[0])

        val eval = state.evaluations[0]
        assertTrue(eval.pitchCorrect)
        assertFalse("200ms on 1.0s note must not be durationCorrect", eval.durationCorrect)
        assertEquals(0.200f, eval.playedDurationSec, 0.001f)

        // Perfect score would be ~100. With 200ms, durationPoints = (0.2 * 70) = 14 -> clamped to 25.
        // Pitch = 100 * 0.6 = 60, Duration = 25 * 0.4 = 10 -> combined = 70.
        assertNotNull(state.overallScore)
        assertTrue("Score with timing error must be significantly degraded (expected ~70)", state.overallScore!! in 65..75)
    }

    // Tier 2: Boundary - Zero duration / extreme short duration handling
    @Test
    fun testZeroDurationHandlingInScoringEngine() {
        val evals = listOf(
            EvaluationRecord(
                noteId = "z1", expectedMidi = 69, pitchCorrect = true,
                durationCorrect = false, centsDeviation = 0,
                expectedDurationSec = 1.0f, playedDurationSec = 0.0f, wrongAttemptsCount = 0
            ),
            EvaluationRecord(
                noteId = "z2", expectedMidi = 71, pitchCorrect = true,
                durationCorrect = false, centsDeviation = 0,
                expectedDurationSec = 0.0f, playedDurationSec = 0.0f, wrongAttemptsCount = 0
            )
        )
        // ScoringEngine must not divide by zero or crash
        val score = ScoringEngine.calculateOverallScore(2, evals, totalWrongAttempts = 0)
        assertTrue("Score with zero duration must be clamped between 0 and 100", score in 0..100)
    }

    // Tier 2: Boundary - Extreme pitch deviation penalty capping
    @Test
    fun testExtremePitchDeviationPenalties() {
        val extremeCentsEvals = listOf(
            EvaluationRecord(
                noteId = "e1", expectedMidi = 69, pitchCorrect = true,
                durationCorrect = true, centsDeviation = 80, // extreme deviation
                expectedDurationSec = 1.0f, playedDurationSec = 1.0f, wrongAttemptsCount = 0
            )
        )
        val score = ScoringEngine.calculateOverallScore(1, extremeCentsEvals, totalWrongAttempts = 0)
        // Pitch points: 100 - min(20, 80 * 0.4) = 80. Duration: 100.
        // Combined: 80 * 0.6 + 100 * 0.4 = 48 + 40 = 88.
        assertEquals(88, score)
    }

    // Tier 4: Real-World Scenario - Simulated full violin score practice session
    @Test
    fun testSimulatedFullViolinScorePracticeSession() {
        val parsedScore = MusicXmlParser.parse(SAMPLE_D_MAJOR_SCALE_XML)
        val nonRestNotes = parsedScore.notes.filter { !it.isRest }
        assertTrue("D Major scale should have multiple notes", nonRestNotes.size >= 8)

        val engine = PracticeEngine(parsedScore.notes, tolerancePercent = 20)
        var currentTime = 10000L

        // Simulate practicing each note:
        // - First 4 notes: Perfect timing (100% duration)
        // - Next 2 notes: Wrong pitch attempt first, then correct pitch played with correct timing
        // - Remaining notes: Correct pitch but short timing (200ms on ~0.75-1.0s)
        for (i in parsedScore.notes.indices) {
            val note = parsedScore.notes[i]
            if (note.isRest) {
                engine.processAudioInput(null, timestampMs = currentTime)
                currentTime += 100L
                continue
            }

            if (i in 4..5) {
                // Wrong pitch attempt
                engine.processAudioInput(mockPitch(note.midi + 2), timestampMs = currentTime)
                currentTime += 50L
            }

            // Play correct pitch
            engine.processAudioInput(mockPitch(note.midi), timestampMs = currentTime)
            val holdDurationMs = if (i < 4) {
                (note.expectedDurationSec * 1000L).toLong()
            } else if (i in 4..5) {
                (note.expectedDurationSec * 1000L).toLong()
            } else {
                200L // timing error (short note)
            }

            currentTime += holdDurationMs
            engine.processAudioInput(null, timestampMs = currentTime)
            currentTime += 50L
        }

        val finalState = engine.getState()
        assertEquals(EngineStatus.FINISHED, finalState.status)
        assertEquals(parsedScore.notes.size, finalState.currentIndex)
        assertEquals(parsedScore.notes.size, finalState.evaluations.size)
        assertEquals(2, finalState.totalWrongAttempts)

        val finalScore = finalState.overallScore
        assertNotNull("Final score must be calculated at end of score", finalScore)
        assertTrue("Final score out of 100 must be in [0, 100]", finalScore!! in 0..100)
        assertTrue("Score with mixed accuracy and 2 wrong attempts should be between 70 and 95", finalScore in 70..95)
    }
}
