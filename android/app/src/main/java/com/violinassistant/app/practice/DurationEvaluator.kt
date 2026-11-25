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

    /** Rest notes cap tolerance at 20% so generous note-hold tolerance does not skip rests too early. */
    fun minRestDuration(expectedSec: Float, tolerancePercent: Int = 20): Float {
        val restTolerance = tolerancePercent.coerceAtMost(20)
        return (expectedSec * (1f - restTolerance / 100f)).coerceAtLeast(0.15f)
    }
}
