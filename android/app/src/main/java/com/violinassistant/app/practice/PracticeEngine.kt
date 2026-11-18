package com.violinassistant.app.practice

import com.violinassistant.app.audio.FrequencyStackDebouncer
import com.violinassistant.app.model.NoteEvent
import com.violinassistant.app.model.NoteStatus
import com.violinassistant.app.model.PitchResult
import com.violinassistant.app.model.PracticeMode

class PracticeEngine(
    private val notes: List<NoteEvent>,
    private val tolerancePercent: Int = 20,
    private val practiceMode: PracticeMode = PracticeMode.GUIDED,
    private val debouncer: FrequencyStackDebouncer? = null,
    private val onStateChange: ((PracticeEngineState) -> Unit)? = null
) {
    private var currentIndex = 0
    private val noteStatuses = mutableMapOf<Int, NoteStatus>()
    private val evaluations = mutableListOf<EvaluationRecord>()
    private var totalWrongAttempts = 0
    private var currentNoteWrongCount = 0
    private var status = EngineStatus.IDLE

    private var notePitchStartTimestamp: Long? = null
    private var lastPitchCents: Int = 0
    private var sessionStartTime: Long = 0L

    init {
        reset()
    }

    fun reset() {
        currentIndex = 0
        noteStatuses.clear()
        evaluations.clear()
        totalWrongAttempts = 0
        currentNoteWrongCount = 0
        notePitchStartTimestamp = null
        lastPitchCents = 0
        debouncer?.reset()
        sessionStartTime = System.currentTimeMillis()
        status = if (notes.isNotEmpty()) EngineStatus.WAITING_FOR_NOTE else EngineStatus.FINISHED

        if (notes.isNotEmpty()) {
            noteStatuses[0] = NoteStatus.ACTIVE
        }
        notifyState(null)
    }

    fun processAudioInput(pitchResult: PitchResult?) {
        if (status == EngineStatus.FINISHED || status == EngineStatus.IDLE || currentIndex >= notes.size) {
            return
        }

        val currentNote = notes[currentIndex]

        // Rest notes advance automatically
        if (currentNote.isRest) {
            advanceCurrentNote(true, true, 0, currentNote.expectedDurationSec)
            notifyState(pitchResult)
            return
        }

        // Silence or unstable pitch
        if (pitchResult == null || !pitchResult.isStable) {
            if (notePitchStartTimestamp != null) {
                val sustainedSec = (System.currentTimeMillis() - notePitchStartTimestamp!!) / 1000f
                notePitchStartTimestamp = null

                val durResult = DurationEvaluator.evaluate(currentNote.expectedDurationSec, sustainedSec, tolerancePercent)
                if (durResult.isAcceptable) {
                    advanceCurrentNote(true, true, lastPitchCents, sustainedSec)
                } else {
                    advanceCurrentNote(true, false, lastPitchCents, sustainedSec)
                }
            }
            notifyState(pitchResult)
            return
        }

        val playedMidi = debouncer?.filterMidi(pitchResult.midi) ?: pitchResult.midi
        val expectedMidi = currentNote.midi
        lastPitchCents = pitchResult.centsDeviation

        if (playedMidi == expectedMidi) {
            // Correct pitch confirmed
            if (notePitchStartTimestamp == null) {
                notePitchStartTimestamp = System.currentTimeMillis()
                status = EngineStatus.MEASURING_DURATION
            }

            val sustainedSec = (System.currentTimeMillis() - notePitchStartTimestamp!!) / 1000f
            val minAcceptable = (currentNote.expectedDurationSec * (1f - tolerancePercent / 100f)).coerceAtLeast(0.15f)

            if (sustainedSec >= minAcceptable) {
                val durResult = DurationEvaluator.evaluate(currentNote.expectedDurationSec, sustainedSec, tolerancePercent)
                notePitchStartTimestamp = null
                advanceCurrentNote(true, durResult.isAcceptable, pitchResult.centsDeviation, sustainedSec)
            }
        } else {
            // Wrong pitch
            notePitchStartTimestamp = null
            noteStatuses[currentIndex] = NoteStatus.WRONG_PITCH
            totalWrongAttempts++
            currentNoteWrongCount++
            debouncer?.startDebounce()
            status = EngineStatus.WAITING_FOR_NOTE
        }

        notifyState(pitchResult)
    }

    private fun advanceCurrentNote(
        pitchCorrect: Boolean,
        durationCorrect: Boolean,
        centsDeviation: Int,
        playedDurationSec: Float
    ) {
        val note = notes[currentIndex]

        evaluations.add(
            EvaluationRecord(
                noteId = note.id,
                expectedMidi = note.midi,
                pitchCorrect = pitchCorrect,
                durationCorrect = durationCorrect,
                centsDeviation = centsDeviation,
                expectedDurationSec = note.expectedDurationSec,
                playedDurationSec = playedDurationSec,
                wrongAttemptsCount = currentNoteWrongCount
            )
        )

        noteStatuses[currentIndex] = if (pitchCorrect && durationCorrect) {
            status = EngineStatus.COMPLETED_CORRECTLY
            NoteStatus.CORRECT
        } else {
            status = EngineStatus.COMPLETED_WITH_ERROR
            NoteStatus.WRONG_DURATION
        }

        currentIndex++
        currentNoteWrongCount = 0
        notePitchStartTimestamp = null
        debouncer?.startDebounce()

        if (currentIndex >= notes.size) {
            status = EngineStatus.FINISHED
        } else {
            noteStatuses[currentIndex] = NoteStatus.ACTIVE
            status = EngineStatus.WAITING_FOR_NOTE
        }
    }

    fun getState(detectedPitch: PitchResult? = null): PracticeEngineState {
        val isFinished = status == EngineStatus.FINISHED || currentIndex >= notes.size
        val score = if (isFinished) {
            ScoringEngine.calculateOverallScore(notes.size, evaluations, totalWrongAttempts)
        } else null

        val currentNote = if (currentIndex < notes.size) notes[currentIndex] else null

        return PracticeEngineState(
            status = status,
            currentIndex = currentIndex,
            totalNotes = notes.size,
            noteStatuses = noteStatuses.toMap(),
            evaluations = evaluations.toList(),
            totalWrongAttempts = totalWrongAttempts,
            overallScore = score,
            currentExpectedNote = currentNote,
            detectedPitch = detectedPitch,
            sessionStartTime = sessionStartTime,
            sessionDurationSec = (System.currentTimeMillis() - sessionStartTime) / 1000L
        )
    }

    private fun notifyState(pitchResult: PitchResult?) {
        onStateChange?.invoke(getState(pitchResult))
    }
}
