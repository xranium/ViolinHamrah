package com.violinassistant.app.model

enum class ScoreDifficulty {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED
}

enum class ScoreCategory {
    CLASSICAL,
    IRANIAN,
    EXERCISES,
    FOLK
}

data class ScoreMetadata(
    val id: String,
    val title: String,
    val composer: String,
    val difficulty: ScoreDifficulty,
    val category: ScoreCategory,
    val description: String = "",
    val version: Int = 1,
    val file: String = "",
    val isOffline: Boolean = true
)
