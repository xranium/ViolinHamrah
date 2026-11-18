package com.violinassistant.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "practice_sessions")
data class PracticeSessionEntity(
    @PrimaryKey val id: String,
    val scoreId: String,
    val scoreTitle: String,
    val date: String, // ISO String
    val durationSec: Long,
    val score: Int, // 0 to 100
    val completedNotes: Int,
    val totalNotes: Int
)
