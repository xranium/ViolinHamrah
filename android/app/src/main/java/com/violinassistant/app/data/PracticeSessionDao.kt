package com.violinassistant.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PracticeSessionDao {
    @Query("SELECT * FROM practice_sessions ORDER BY date DESC")
    fun getAllSessionsFlow(): Flow<List<PracticeSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: PracticeSessionEntity)

    @Query("SELECT COUNT(*) FROM practice_sessions")
    suspend fun getSessionCount(): Int
}
