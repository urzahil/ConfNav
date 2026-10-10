package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.ConferenceSession

@Database(entities = [ConferenceSession::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conferenceDao(): ConferenceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "conference_navigator_db"
                )
                    // Do not silently destroy user data if a future schema migration is missing.
                    // Add an explicit Migration whenever the Room schema version changes.
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
