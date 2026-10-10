package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ConferenceSession
import kotlinx.coroutines.flow.Flow

@Dao
interface ConferenceDao {
    @Query("SELECT * FROM conference_sessions ORDER BY date ASC, startTime ASC")
    fun getAllSessions(): Flow<List<ConferenceSession>>

    @Query("SELECT * FROM conference_sessions WHERE id = :id")
    fun getSessionById(id: Long): Flow<ConferenceSession?>

    @Query("SELECT * FROM conference_sessions WHERE date = :date ORDER BY startTime ASC")
    fun getSessionsByDate(date: String): Flow<List<ConferenceSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ConferenceSession): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<ConferenceSession>)

    @Update
    suspend fun updateSession(session: ConferenceSession)

    @Delete
    suspend fun deleteSession(session: ConferenceSession)

    @Query("DELETE FROM conference_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)

    @Query("DELETE FROM conference_sessions")
    suspend fun clearAll()

    @Query("DELETE FROM conference_sessions WHERE title IN (:titles)")
    suspend fun deleteByTitles(titles: List<String>)

    @Query("SELECT COUNT(*) FROM conference_sessions")
    suspend fun getCount(): Int
}
