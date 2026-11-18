package com.violinassistant.app.model

data class PitchResult(
    val frequency: Float,
    val midi: Int,
    val pitchName: String,
    val centsDeviation: Int,
    val clarity: Float,
    val volume: Float,
    val isStable: Boolean
)

enum class PracticeMode {
    GUIDED,
    RHYTHM
}

data class AppSettings(
    val tuningRefHz: Int = 440,
    val practiceMode: PracticeMode = PracticeMode.GUIDED,
    val durationTolerancePercent: Int = 20,
    val githubOwner: String = "opensheetmusicdisplay",
    val githubRepo: String = "opensheetmusicdisplay",
    val githubBranch: String = "develop",
    val manifestPath: String = "test/data/manifest.json"
)
