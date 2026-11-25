package com.violinassistant.app.practice

data class DurationCheckResult(
    val isAcceptable: Boolean,
    val isTooShort: Boolean,
    val isTooLong: Boolean,
    val playedDurationSec: Float
)

object DurationEvaluator {
    fun evaluate(
        expectedSec: Float,
        playedSec: Float,
        tolerancePercent: Int = 20
    ): DurationCheckResult {
        val tolerance = tolerancePercent / 100f
        val minDuration = (expectedSec * (1f - tolerance)).coerceAtLeast(0.15f)
        val maxDuration = (expectedSec * (1f + tolerance * 1.6f)).coerceAtLeast(0.30f)

        val isTooShort = playedSec < minDuration
        val isTooLong = playedSec > maxDuration
        val isAcceptable = !isTooShort && !isTooLong

        return DurationCheckResult(
            isAcceptable = isAcceptable,
            isTooShort = isTooShort,
            isTooLong = isTooLong,
            playedDurationSec = playedSec
        )
    }

    /**
     * Continuous-hold auto-advance caps early lead at 0.22s so long notes (half/whole)
     * stay aligned with the metronome beat instead of finishing multiple beats early.
     */
    fun minAutoAdvanceDuration(expectedSec: Float, tolerancePercent: Int = 20): Float {
        val leadSec = (expectedSec * (tolerancePercent / 100f)).coerceAtMost(0.22f)
        return (expectedSec - leadSec).coerceAtLeast(0.15f)
    }

    /** Rest notes cap tolerance at 20% and lead at 0.22s to stay locked with the metronome. */
    fun minRestDuration(expectedSec: Float, tolerancePercent: Int = 20): Float {
        val restTolerance = tolerancePercent.coerceAtMost(20)
        val leadSec = (expectedSec * (restTolerance / 100f)).coerceAtMost(0.22f)
        return (expectedSec - leadSec).coerceAtLeast(0.15f)
    }
}
