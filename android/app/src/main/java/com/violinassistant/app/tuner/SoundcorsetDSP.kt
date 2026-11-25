package com.violinassistant.app.tuner

import kotlin.math.abs

/**
 * Exact 4-stage dyadic box filter and lag-searched parabolic autocorrelation detector
 * from Soundcorset (AutoCorrelationPitchDetector, SoundUtil, FloatingMaxVolume).
 */
object SoundcorsetDSP {
    fun getVolume(samples: DoubleArray): Double {
        val limit = minOf(samples.size, 500)
        var maxAmp = 0.0
        var i = 0
        while (i < limit) {
            val v = abs(samples[i])
            if (v > maxAmp) maxAmp = v
            i += 3
        }
        return maxAmp * 100.0
    }

    fun boxFilter4Stages(samples: DoubleArray): DoubleArray {
        var cur = samples
        var stride = 1
        for (stage in 0 until 4) {
            val nextLen = cur.size - stride
            if (nextLen <= 32) break
            val next = DoubleArray(nextLen)
            for (i in 0 until nextLen) {
                next[i] = (cur[i] + cur[i + stride]) * 0.5
            }
            cur = next
            stride *= 2
        }
        return cur
    }

    fun autoCorrelation(samples: DoubleArray, lag: Int): Double {
        val len = samples.size - lag
        if (lag < 0 || len <= 0) return -1e26
        var sum = 0.0
        for (i in 0 until len) {
            sum += samples[i] * samples[i + lag]
        }
        return sum / len.toDouble()
    }

    fun rangeSearch(samples: DoubleArray, centerLag: Int): Pair<Int, Double> {
        val step = when {
            centerLag > 2080 -> 32
            centerLag > 1040 -> 16
            centerLag > 520 -> 8
            centerLag > 260 -> 4
            centerLag > 130 -> 2
            else -> 1
        }
        val halfSteps = if (centerLag > 33) 4 else 2
        var bestLag = centerLag
        var bestAc = -1e11
        var offset = -halfSteps * step
        val endOffset = halfSteps * step
        while (offset <= endOffset) {
            val lag = (centerLag + offset).coerceIn(1, samples.size - 2)
            val ac = autoCorrelation(samples, lag)
            if (ac > bestAc) {
                bestAc = ac
                bestLag = lag
            }
            offset += step
        }
        return bestLag to bestAc
    }

    fun fineTuneHz(samples: DoubleArray, lag: Int, sampleRate: Int): Double {
        val safeLag = lag.coerceIn(2, samples.size - 3)
        val yMinus = autoCorrelation(samples, safeLag - 1)
        val yZero = autoCorrelation(samples, safeLag)
        val yPlus = autoCorrelation(samples, safeLag + 1)
        val denom = 2.0 * (2.0 * yZero - yMinus - yPlus)
        val rawDelta = if (abs(denom) > 1e-18) (yPlus - yMinus) / denom else 0.0
        val delta = rawDelta.coerceIn(-1.0, 1.0)
        return sampleRate.toDouble() / (safeLag.toDouble() + delta)
    }

    fun detectPitchHz(
        samples: DoubleArray,
        sampleRate: Int,
        noteFrequencies: DoubleArray,
        noteLags: IntArray
    ): Double {
        val filtered = boxFilter4Stages(samples)
        val numNotes = noteFrequencies.size
        val acResults = DoubleArray(numNotes)
        var maxPos = 0
        var maxAc = -1e26

        for (i in 0 until numNotes) {
            val ac = autoCorrelation(filtered, noteLags[i])
            acResults[i] = ac
            if (maxAc < ac && i > 0) {
                if (i != numNotes - 1) {
                    maxAc = ac
                    maxPos = i
                } else if (maxPos == i - 1) {
                    return SoundcorsetFrequencyPool.INVALID_FREQ
                }
            }
        }

        var chosenLag: Int? = null
        var p = numNotes - 2
        while (p > maxPos) {
            if (acResults[p - 1] < acResults[p] &&
                acResults[p + 1] < acResults[p] &&
                maxAc < acResults[p] * 1.4
            ) {
                val (candLag, candVal) = rangeSearch(filtered, noteLags[p])
                if (maxAc < 1.08 * candVal) {
                    chosenLag = candLag
                    break
                }
            }
            p--
        }

        val finalLag = chosenLag ?: rangeSearch(filtered, noteLags[maxPos]).first
        return fineTuneHz(filtered, finalLag, sampleRate)
    }
}

class FloatingMaxVolumeTracker {
    var baseVolume: Double = -1.99
        private set

    fun update(volume: Double) {
        if (baseVolume < 0.0) {
            baseVolume += 1.0
        } else {
            baseVolume = maxOf(volume, baseVolume) * 0.96
        }
    }

    fun isAboveGate(volume: Double): Boolean =
        volume >= 0.1 && volume >= 0.3 * baseVolume

    fun reset() {
        baseVolume = -1.99
    }
}
