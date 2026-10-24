package rocks.urzahil.confnav.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import rocks.urzahil.confnav.data.model.ConferenceSession

// exportSchema is on so every schema version is written to app/schemas (see build.gradle.kts).
// Commit those files and add an explicit Migration whenever `version` changes.
@Database(entities = [ConferenceSession::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conferenceDao(): ConferenceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "conference_navigator_db"
                )
                    // Do not silently destroy user data if a future schema migration is missing.
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
