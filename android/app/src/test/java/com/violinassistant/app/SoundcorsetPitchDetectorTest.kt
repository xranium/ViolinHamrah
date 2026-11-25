package com.violinassistant.app

import com.violinassistant.app.tuner.SoundcorsetDSP
import com.violinassistant.app.tuner.SoundcorsetFrequencyPool
import com.violinassistant.app.tuner.SoundcorsetTunerEngine
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class SoundcorsetPitchDetectorTest {
    private val sampleRate = 44100
    private val frameSize = 4410

    private fun sineWave(freqHz: Double, amplitude: Double = 0.6): DoubleArray =
        DoubleArray(frameSize) { i ->
            amplitude * sin(2.0 * PI * freqHz * i / sampleRate)
        }

    private fun violinHarmonicWave(freqHz: Double, amplitude: Double = 0.5): DoubleArray =
        DoubleArray(frameSize) { i ->
            val phase = 2.0 * PI * freqHz * i / sampleRate
            amplitude * (0.40 * sin(phase) + 0.55 * sin(2.0 * phase) + 0.30 * sin(3.0 * phase))
        }

    @Test
    fun `detects all open violin strings accurately`() {
        val freqs = SoundcorsetFrequencyPool.buildNoteFrequencies(440.0f)
        val lags = SoundcorsetFrequencyPool.buildNoteLagTable(freqs, sampleRate)
        val targets = listOf(196.00 to "G3", 293.66 to "D4", 440.00 to "A4", 659.25 to "E5")

        for ((targetHz, expectedName) in targets) {
            val detected = SoundcorsetDSP.detectPitchHz(sineWave(targetHz), sampleRate, freqs, lags)
            val idx = SoundcorsetFrequencyPool.closestNoteIndex(detected, freqs)
            val name = SoundcorsetFrequencyPool.noteName(idx, useFlat = false, useSolfege = false, includeOctave = true)
            val cents = SoundcorsetDSP.let { SoundcorsetFrequencyPool.diffInCents(freqs[idx], detected) }
            assertEquals("Expected $expectedName for $targetHz Hz", expectedName, name)
            assertTrue("Expected within 3 cents for $targetHz Hz, got $cents", abs(cents) < 3.0)
        }
    }

    @Test
    fun `rejects second harmonic octave jump on rich violin waves`() {
        val freqs = SoundcorsetFrequencyPool.buildNoteFrequencies(440.0f)
        val lags = SoundcorsetFrequencyPool.buildNoteLagTable(freqs, sampleRate)

        val g3 = SoundcorsetDSP.detectPitchHz(violinHarmonicWave(196.00), sampleRate, freqs, lags)
        assertTrue("Expected G3 fundamental ~196Hz, got $g3", abs(g3 - 196.0) < 2.0)

        val d4 = SoundcorsetDSP.detectPitchHz(violinHarmonicWave(293.66), sampleRate, freqs, lags)
        assertTrue("Expected D4 fundamental ~293.66Hz, got $d4", abs(d4 - 293.66) < 2.5)
    }

    @Test
    fun `respects A4 442Hz calibration and flat sharp formatting`() {
        val freqs442 = SoundcorsetFrequencyPool.buildNoteFrequencies(442.0f)
        val lags442 = SoundcorsetFrequencyPool.buildNoteLagTable(freqs442, sampleRate)
        val detected = SoundcorsetDSP.detectPitchHz(sineWave(442.0), sampleRate, freqs442, lags442)
        val cents = SoundcorsetFrequencyPool.diffInCents(freqs442[SoundcorsetFrequencyPool.A4_INDEX], detected)
        assertTrue("442Hz should center near 0c at A4=442 calibration, got $cents", abs(cents) < 2.0)

        val bFlatIdx = SoundcorsetFrequencyPool.A4_INDEX + 1 // Bb4 / A#4
        assertEquals("A4\u266F", SoundcorsetFrequencyPool.noteName(bFlatIdx, useFlat = false, useSolfege = false, includeOctave = true))
        assertEquals("B4\u266D", SoundcorsetFrequencyPool.noteName(bFlatIdx, useFlat = true, useSolfege = false, includeOctave = true))
        assertEquals("La", SoundcorsetFrequencyPool.noteName(SoundcorsetFrequencyPool.A4_INDEX, useFlat = false, useSolfege = true, includeOctave = false))
    }

    @Test
    fun `tuner engine scores sustained in-tune frames and detects noisy confusion`() {
        val engine = SoundcorsetTunerEngine(initialSampleRate = sampleRate)
        val a4 = sineWave(440.0, amplitude = 0.65)
        // First 2 frames warm up FloatingMaxVolume baseVolume (-1.99 -> -0.99 -> +0.01)
        repeat(14) { step ->
            engine.processAudioFrame(a4, nowMs = 1000L + step * 100L)
        }
        val tunedState = engine.state.value
        assertTrue("Expected active note after sustained A4", tunedState.hasActiveNote)
        assertTrue("Expected score >= 10 after 12 live A4 frames, got ${tunedState.score}", tunedState.score >= 10)
        assertEquals(0.0, tunedState.confusionLevel, 0.05)

        // Rapid alternating distant pitches should trigger confusion ("NOISY")
        val erraticFreqs = listOf(196.0, 440.0, 659.25, 293.66, 523.25)
        erraticFreqs.forEachIndexed { idx, f ->
            engine.processAudioFrame(sineWave(f, amplitude = 0.65), nowMs = 3000L + idx * 100L)
        }
        assertTrue("Expected confusionLevel > 0.03 after erratic pitch switches", engine.state.value.confusionLevel > 0.03)

        // Silence falls below floating volume gate
        val silence = DoubleArray(frameSize) { 0.0001 }
        engine.processAudioFrame(silence, nowMs = 4000L)
        assertFalse("Silence must not be marked live", engine.state.value.isLive)
    }
}
