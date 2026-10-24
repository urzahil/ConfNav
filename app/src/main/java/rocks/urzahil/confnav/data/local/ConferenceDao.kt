package rocks.urzahil.confnav.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import rocks.urzahil.confnav.data.model.ConferenceSession
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ConferenceDao {
    @Query("SELECT * FROM conference_sessions ORDER BY date ASC, startTime ASC")
    abstract fun getAllSessions(): Flow<List<ConferenceSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertSession(session: ConferenceSession): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertSessions(sessions: List<ConferenceSession>)

    @Update
    abstract suspend fun updateSession(session: ConferenceSession)

    @Query("DELETE FROM conference_sessions WHERE id = :id")
    abstract suspend fun deleteSessionById(id: Long)

    @Query("DELETE FROM conference_sessions")
    abstract suspend fun clearAll()

    /** Clears and re-inserts in one transaction, so a failed insert never leaves the agenda empty. */
    @Transaction
    open suspend fun replaceAll(sessions: List<ConferenceSession>) {
        clearAll()
        insertSessions(sessions)
    }
}
