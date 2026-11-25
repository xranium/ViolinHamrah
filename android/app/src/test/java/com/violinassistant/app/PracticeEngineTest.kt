package com.violinassistant.app

import com.violinassistant.app.audio.FrequencyStackDebouncer
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
        engine.processAudioInput(mockPitch(62), timestampMs = 1000L)
        assertEquals(0, engine.getState().currentIndex)

        // 2. Play correct note
        engine.processAudioInput(mockPitch(60), timestampMs = 1100L)
        assertEquals(0, engine.getState().currentIndex)

        // 3. Release note after sustaining > 20ms
        engine.processAudioInput(null, timestampMs = 1200L)
        assertEquals(1, engine.getState().currentIndex)
    }

    @Test
    fun testTransientNoiseUnder20msDoesNotAdvance() {
        // 10 ms transient noise: correct pitch starts at 1000ms
        engine.processAudioInput(mockPitch(60), timestampMs = 1000L)
        assertEquals(0, engine.getState().currentIndex)

        // Drops to silence at 1010ms (10ms < 20ms)
        engine.processAudioInput(null, timestampMs = 1010L)
        val state = engine.getState()
        assertEquals(0, state.currentIndex)
        assertEquals(NoteStatus.ACTIVE, state.noteStatuses[0])
    }

    @Test
    fun testNoteSustainedAtLeast20msAdvances() {
        // Correct pitch starts at 1000ms
        engine.processAudioInput(mockPitch(60), timestampMs = 1000L)
        assertEquals(0, engine.getState().currentIndex)

        // Released at 1025ms (25ms >= 20ms transient threshold)
        engine.processAudioInput(null, timestampMs = 1025L)
        val state = engine.getState()
        assertEquals(1, state.currentIndex)
    }

    @Test
    fun testIncorrectTimingYieldsWrongDuration() {
        // Expected is 0.8s. User sustains for only 200ms then releases.
        engine.processAudioInput(mockPitch(60), timestampMs = 1000L)
        engine.processAudioInput(null, timestampMs = 1200L)

        val state = engine.getState()
        assertEquals(1, state.currentIndex)
        assertEquals(NoteStatus.WRONG_DURATION, state.noteStatuses[0])
        assertEquals(1, state.evaluations.size)
        val evaluation = state.evaluations[0]
        assertEquals(true, evaluation.pitchCorrect)
        assertEquals(false, evaluation.durationCorrect)
    }

    @Test
    fun testCorrectTimingYieldsCorrectStatus() {
        // Expected is 0.8s. User sustains for 800ms.
        engine.processAudioInput(mockPitch(60), timestampMs = 1000L)
        engine.processAudioInput(mockPitch(60), timestampMs = 1800L)

        val state = engine.getState()
        assertEquals(1, state.currentIndex)
        assertEquals(NoteStatus.CORRECT, state.noteStatuses[0])
        assertEquals(1, state.evaluations.size)
        val evaluation = state.evaluations[0]
        assertEquals(true, evaluation.pitchCorrect)
        assertEquals(true, evaluation.durationCorrect)
    }

    @Test
    fun testMidNoteVibrationGlitchDoesNotBreakDurationOrTriggerWrongNote() {
        val forgivingEngine = PracticeEngine(
            notes = notes,
            tolerancePercent = 45,
            debouncer = FrequencyStackDebouncer(debounceCooldownMs = 120L, minConsecutiveFrames = 2),
            glitchGraceMs = 280L
        )

        // Start C4 (expected 0.8s; 45% tolerance -> 0.44s needed)
        forgivingEngine.processAudioInput(mockPitch(60), timestampMs = 1000L)
        forgivingEngine.processAudioInput(mockPitch(60), timestampMs = 1025L) // confirmed C4

        // Mid-note bow vibration flickers onto wrong pitch / silence for ~180ms (< 280ms grace)
        forgivingEngine.processAudioInput(mockPitch(61), timestampMs = 1150L)
        forgivingEngine.processAudioInput(mockPitch(61), timestampMs = 1175L)
        forgivingEngine.processAudioInput(null, timestampMs = 1220L)
        assertEquals(0, forgivingEngine.getState().totalWrongAttempts)
        assertEquals(0, forgivingEngine.getState().currentIndex)

        // Recover onto C4 and reach sustained threshold (0.8s - 0.22s cap = 0.58s)
        forgivingEngine.processAudioInput(mockPitch(60), timestampMs = 1260L)
        forgivingEngine.processAudioInput(mockPitch(60), timestampMs = 1285L)
        forgivingEngine.processAudioInput(mockPitch(60), timestampMs = 1610L)

        val state = forgivingEngine.getState()
        assertEquals(1, state.currentIndex)
        assertEquals(NoteStatus.CORRECT, state.noteStatuses[0])
        assertEquals(0, state.totalWrongAttempts)
    }

    @Test
    fun testLongNoteDoesNotAdvanceMultipleBeatsEarly() {
        val wholeNote = NoteEvent(
            id = "w1", step = "A", alter = 0, octave = 4, midi = 69,
            pitchName = "A4", durationInBeats = 4f, expectedDurationSec = 3.69f,
            measure = 1, voice = 1, isRest = false
        )
        val wholeEngine = PracticeEngine(listOf(wholeNote), tolerancePercent = 45)
        wholeEngine.processAudioInput(mockPitch(69), timestampMs = 1000L)
        wholeEngine.processAudioInput(mockPitch(69), timestampMs = 3100L) // 2.10s (57%): must still hold
        assertEquals(0, wholeEngine.getState().currentIndex)
        wholeEngine.processAudioInput(mockPitch(69), timestampMs = 4480L) // 3.48s >= 3.47s
        assertEquals(1, wholeEngine.getState().currentIndex)
        assertEquals(NoteStatus.CORRECT, wholeEngine.getState().noteStatuses[0])
    }

    @Test
    fun testRealWrongPitchAfterGraceStillMarksWrongAttempt() {
        val forgivingEngine = PracticeEngine(
            notes = notes,
            tolerancePercent = 45,
            debouncer = FrequencyStackDebouncer(debounceCooldownMs = 120L, minConsecutiveFrames = 2),
            glitchGraceMs = 280L
        )
        // Sustained wrong pitch D4 (62) with no prior C4 hold triggers WRONG_PITCH cleanly
        forgivingEngine.processAudioInput(mockPitch(62), timestampMs = 2000L)
        forgivingEngine.processAudioInput(mockPitch(62), timestampMs = 2025L)

        val state = forgivingEngine.getState()
        assertEquals(0, state.currentIndex)
        assertEquals(NoteStatus.WRONG_PITCH, state.noteStatuses[0])
        assertEquals(1, state.totalWrongAttempts)
    }

    @Test
    fun testQuietNoiseDoesNotTriggerWrongNoteAndAdvanceClearsDetectedPitch() {
        val gatedEngine = PracticeEngine(notes = notes, tolerancePercent = 45, minWrongNoteVolume = 0.03f)
        val quietNoise = PitchResult(440f, 65, "F4", 0, 0.95f, volume = 0.01f, isStable = true)
        gatedEngine.processAudioInput(quietNoise, timestampMs = 1000L)
        assertEquals(0, gatedEngine.getState().totalWrongAttempts)
        assertEquals(NoteStatus.ACTIVE, gatedEngine.getState().noteStatuses[0])

        var lastNotifiedPitch: PitchResult? = mockPitch(60)
        val advanceEngine = PracticeEngine(notes = notes, tolerancePercent = 20) { s -> lastNotifiedPitch = s.detectedPitch }
        advanceEngine.processAudioInput(mockPitch(60), timestampMs = 1000L)
        advanceEngine.processAudioInput(mockPitch(60), timestampMs = 1800L)
        assertEquals(1, advanceEngine.getState().currentIndex)
        assertEquals(null, lastNotifiedPitch)
    }
}
