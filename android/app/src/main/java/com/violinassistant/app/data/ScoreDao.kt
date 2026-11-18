package com.violinassistant.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScoreDao {
    @Query("SELECT * FROM scores ORDER BY title ASC")
    fun getAllScoresFlow(): Flow<List<ScoreEntity>>

    @Query("SELECT * FROM scores WHERE id = :id LIMIT 1")
    suspend fun getScoreById(id: String): ScoreEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(score: ScoreEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(scores: List<ScoreEntity>)

    @Query("SELECT COUNT(*) FROM scores")
    suspend fun getScoreCount(): Int
}
