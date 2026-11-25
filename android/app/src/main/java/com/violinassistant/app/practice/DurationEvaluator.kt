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
     * Continuous-hold auto-advance threshold.
     * When [exactBeatSync] is true, advances within one 25ms frame of [expectedSec] so the cursor
     * snaps on the exact metronome beat; otherwise caps early lead at 0.22s.
     */
    fun minAutoAdvanceDuration(
        expectedSec: Float,
        tolerancePercent: Int = 20,
        exactBeatSync: Boolean = false
    ): Float {
        val maxLead = if (exactBeatSync) 0.025f else 0.22f
        val leadSec = (expectedSec * (tolerancePercent / 100f)).coerceAtMost(maxLead)
        return (expectedSec - leadSec).coerceAtLeast(0.15f)
    }

    /** Rest notes cap tolerance at 20% and align with exact beat timing when [exactBeatSync] is enabled. */
    fun minRestDuration(
        expectedSec: Float,
        tolerancePercent: Int = 20,
        exactBeatSync: Boolean = false
    ): Float {
        val restTolerance = tolerancePercent.coerceAtMost(20)
        val maxLead = if (exactBeatSync) 0.025f else 0.22f
        val leadSec = (expectedSec * (restTolerance / 100f)).coerceAtMost(maxLead)
        return (expectedSec - leadSec).coerceAtLeast(0.15f)
    }
}
