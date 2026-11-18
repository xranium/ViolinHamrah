package com.violinassistant.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scores")
data class ScoreEntity(
    @PrimaryKey val id: String,
    val title: String,
    val composer: String,
    val difficulty: String,
    val category: String,
    val description: String,
    val version: Int,
    val xmlContent: String,
    val isOffline: Boolean = true
)
