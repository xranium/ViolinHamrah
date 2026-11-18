package com.violinassistant.app.audio

import com.violinassistant.app.model.PitchResult
import com.violinassistant.app.musicxml.PitchMath
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * High-accuracy Violin Pitch Detector inspired by Trala's native audio engine.
 * Employs McLeod Pitch Method (NSDF) with parabolic interpolation,
 * violin-specific acoustic filtering (G3 to E7), harmonic check, and temporal smoothing.
 */
class ViolinPitchDetector(
    private var tuningRefHz: Int = 440
) {
    companion object {
        const val MIN_VIOLIN_FREQ = 185f  // Just below G3 (196 Hz)
        const val MAX_VIOLIN_FREQ = 2700f // Up to E7 (2637 Hz)
        const val MIN_RMS_VOLUME = 0.015f // Acoustic squelch threshold for violin
        const val CLARITY_THRESHOLD = 0.78f // McLeod confidence threshold
        const val HISTORY_SIZE = 5
    }

    private val pitchHistory = mutableListOf<Float>()

    fun setTuningRef(refHz: Int) {
        this.tuningRefHz = refHz
    }

    fun processSamples(samples: FloatArray, sampleRate: Int): PitchResult? {
        val n = samples.size
        if (n < 512) return null

        // 1. Calculate RMS volume (energy)
        var sumSquares = 0f
        for (i in 0 until n) {
            sumSquares += samples[i] * samples[i]
        }
        val rms = sqrt(sumSquares / n)

        if (rms < MIN_RMS_VOLUME) {
            pitchHistory.clear()
            return null
        }

        // 2. McLeod Pitch Method (NSDF - Normalized Square Difference Function)
        val maxTau = (sampleRate / MIN_VIOLIN_FREQ).toInt().coerceAtMost(n / 2)
        val minTau = (sampleRate / MAX_VIOLIN_FREQ).toInt().coerceAtLeast(2)

        val nsdf = FloatArray(maxTau + 1)
        for (tau in minTau..maxTau) {
            var acf = 0f
            var divisor = 0f
            for (i in 0 until (n - tau)) {
                val s1 = samples[i]
                val s2 = samples[i + tau]
                acf += s1 * s2
                divisor += (s1 * s1) + (s2 * s2)
            }
            nsdf[tau] = if (divisor > 0.00001f) (2f * acf) / divisor else 0f
        }

        // 3. Find key peak above clarity threshold
        var bestTau = -1
        var bestClarity = 0f
        var positiveRegion = false

        for (tau in minTau until maxTau) {
            if (nsdf[tau] > 0f) {
                positiveRegion = true
                if (nsdf[tau] > nsdf[tau - 1] && nsdf[tau] >= nsdf[tau + 1]) {
                    if (nsdf[tau] > bestClarity) {
                        bestClarity = nsdf[tau]
                        bestTau = tau
                        // If clarity exceeds threshold, accept early peak (avoids subharmonic octave doubling)
                        if (bestClarity >= CLARITY_THRESHOLD) break
                    }
                }
            } else if (positiveRegion && bestTau != -1) {
                break
            }
        }

        if (bestTau == -1 || bestClarity < CLARITY_THRESHOLD) {
            return null
        }

        // 4. Parabolic Interpolation for accurate fractional period
        val alpha = nsdf[bestTau - 1]
        val beta = nsdf[bestTau]
        val gamma = nsdf[bestTau + 1]
        val denom = 2f * (2f * beta - alpha - gamma)
        val delta = if (abs(denom) > 0.00001f) (gamma - alpha) / denom else 0f
        val accurateTau = bestTau + delta

        val detectedFreq = sampleRate / accurateTau
        if (detectedFreq < MIN_VIOLIN_FREQ || detectedFreq > MAX_VIOLIN_FREQ) {
            return null
        }

        // 5. Temporal Smoothing (Median Filter to smooth vibrato and reject transient outliers)
        pitchHistory.add(detectedFreq)
        if (pitchHistory.size > HISTORY_SIZE) {
            pitchHistory.removeAt(0)
        }

        val sorted = pitchHistory.sorted()
        val medianFreq = sorted[sorted.size / 2]

        // Stability check: detections must be within 50 cents of median
        var isStable = pitchHistory.size >= 2
        for (p in pitchHistory) {
            val centsDiff = abs(1200f * (ln(p / medianFreq) / ln(2f)))
            if (centsDiff > 50f) {
                isStable = false
                break
            }
        }

        val midiCents = PitchMath.frequencyToMidiAndCents(medianFreq, tuningRefHz)
        val pitchName = PitchMath.midiToPitchName(midiCents.midi)

        return PitchResult(
            frequency = medianFreq,
            midi = midiCents.midi,
            pitchName = pitchName,
            centsDeviation = midiCents.cents,
            clarity = bestClarity,
            volume = rms,
            isStable = isStable
        )
    }

    fun reset() {
        pitchHistory.clear()
    }
}
