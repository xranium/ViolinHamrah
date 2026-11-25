package com.violinassistant.app.tuner

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.round

/**
 * Equal-temperament note frequency table and formatting matching Soundcorset's
 * FrequencyUtil + PitchPool (B0..C7, 74 notes, configurable A4 calibration).
 */
object SoundcorsetFrequencyPool {
    const val NOTE_START = 23 // MIDI B0
    const val A4_INDEX = 46   // 69 - 23
    const val NOTE_COUNT = 74 // B0..C7
    const val DEFAULT_A4_HZ = 440.0f
    const val HISTORY_SIZE = 65
    const val INVALID_FREQ = 99999.0

    private val LN2 = ln(2.0)

    private val ALPHA_SHARP = arrayOf(
        "C", "C\u266f", "D", "D\u266f", "E", "F", "F\u266f", "G", "G\u266f", "A", "A\u266f", "B"
    )
    private val ALPHA_FLAT = arrayOf(
        "C", "D\u266d", "D", "E\u266d", "E", "F", "G\u266d", "G", "A\u266d", "A", "B\u266d", "B"
    )
    private val SOLFEGE_SHARP = arrayOf(
        "Do", "Do\u266f", "Re", "Re\u266f", "Mi", "Fa", "Fa\u266f", "Sol", "Sol\u266f", "La", "La\u266f", "Si"
    )
    private val SOLFEGE_FLAT = arrayOf(
        "Do", "Re\u266d", "Re", "Mi\u266d", "Mi", "Fa", "Sol\u266d", "Sol", "La\u266d", "La", "Si\u266d", "Si"
    )

    fun buildNoteFrequencies(a4Hz: Float = DEFAULT_A4_HZ): DoubleArray =
        DoubleArray(NOTE_COUNT) { i ->
            round(a4Hz.toDouble() * 2.0.pow((i - A4_INDEX) / 12.0) * 100.0) / 100.0
        }

    fun buildNoteLagTable(noteFrequencies: DoubleArray, sampleRate: Int): IntArray =
        IntArray(noteFrequencies.size) { i ->
            (sampleRate.toDouble() / noteFrequencies[i] + 0.5).toInt().coerceAtLeast(1)
        }

    fun closestNoteIndex(freq: Double, noteFrequencies: DoubleArray): Int {
        val clamped = freq.coerceIn(noteFrequencies.first(), noteFrequencies.last())
        var bestIdx = 0
        var bestDiff = Double.MAX_VALUE
        for (i in noteFrequencies.indices) {
            val d = abs(noteFrequencies[i] - clamped)
            if (d < bestDiff) {
                bestDiff = d
                bestIdx = i
            }
        }
        return bestIdx
    }

    fun diffInCents(refFreq: Double, detectedFreq: Double): Double {
        if (refFreq <= 0.0 || detectedFreq <= 0.0) return 0.0
        return 1200.0 * ln(detectedFreq / refFreq) / LN2
    }

    fun noteName(
        index: Int,
        useFlat: Boolean,
        useSolfege: Boolean,
        includeOctave: Boolean
    ): String {
        val safeIdx = index.coerceIn(0, NOTE_COUNT - 1)
        val midi = safeIdx + NOTE_START
        val semitone = midi % 12
        val octave = (midi / 12) - 1
        val table = when {
            useSolfege && useFlat -> SOLFEGE_FLAT
            useSolfege -> SOLFEGE_SHARP
            useFlat -> ALPHA_FLAT
            else -> ALPHA_SHARP
        }
        val base = table[semitone]
        if (!includeOctave) return base
        val lastChar = base.last()
        return if (lastChar == '\u266f' || lastChar == '\u266d') {
            "${base.dropLast(1)}$octave$lastChar"
        } else {
            "$base$octave"
        }
    }
}
