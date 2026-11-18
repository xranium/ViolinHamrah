package com.violinassistant.app.musicxml

import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

data class MidiAndCents(
    val midi: Int,
    val cents: Int,
    val exactMidi: Float
)

object PitchMath {
    private val STEP_OFFSETS = mapOf(
        "C" to 0,
        "D" to 2,
        "E" to 4,
        "F" to 5,
        "G" to 7,
        "A" to 9,
        "B" to 11
    )

    private val NOTE_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    fun calculateMidiPitch(step: String, alter: Int = 0, octave: Int = 4): Int {
        val baseOffset = STEP_OFFSETS[step.uppercase()] ?: 0
        return (octave + 1) * 12 + baseOffset + alter
    }

    fun midiToPitchName(midi: Int): String {
        if (midi <= 0) return "—"
        val noteIndex = ((midi % 12) + 12) % 12
        val octave = (midi / 12) - 1
        return "${NOTE_NAMES[noteIndex]}$octave"
    }

    fun frequencyToMidiAndCents(frequency: Float, tuningRefHz: Int = 440): MidiAndCents {
        if (frequency <= 0f) {
            return MidiAndCents(0, 0, 0f)
        }
        val exactMidi = (69.0 + 12.0 * (ln(frequency.toDouble() / tuningRefHz) / ln(2.0))).toFloat()
        val roundedMidi = exactMidi.roundToInt()
        val cents = ((exactMidi - roundedMidi) * 100).roundToInt()
        return MidiAndCents(roundedMidi, cents, exactMidi)
    }

    fun midiToFrequency(midi: Int, tuningRefHz: Int = 440): Float {
        return (tuningRefHz * 2.0.pow((midi - 69.0) / 12.0)).toFloat()
    }
}
