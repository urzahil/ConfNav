package rocks.urzahil.confnav.data.local

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import rocks.urzahil.confnav.data.model.ConferenceSession

// exportSchema is on so every schema version is written to app/schemas (see build.gradle.kts).
// Commit those files and add a migration (and a case in AppDatabaseMigrationTest) whenever `version` changes.
@Database(
    entities = [ConferenceSession::class],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2, spec = AppDatabase.DropIsBookmarked::class),
        // v3 adds `starred` (defaults to not starred)
        AutoMigration(from = 2, to = 3)
    ]
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conferenceDao(): ConferenceDao

    /** v2 drops `isBookmarked`, which was always true and never shown. */
    @DeleteColumn(tableName = "conference_sessions", columnName = "isBookmarked")
    class DropIsBookmarked : AutoMigrationSpec

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
