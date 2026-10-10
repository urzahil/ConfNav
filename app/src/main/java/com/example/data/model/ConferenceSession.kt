package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conference_sessions")
data class ConferenceSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val speaker: String,
    val speakerRole: String = "",
    val date: String,          // Format: YYYY-MM-DD
    val startTime: String,     // Format: HH:mm (24-hour)
    val endTime: String,       // Format: HH:mm (24-hour)
    val locationName: String,  // e.g. "Main Stage - Hall A", "Workshop Room 204"
    val address: String,       // Street address or venue descriptor
    val latitude: Double,
    val longitude: Double,
    val colorHex: String,      // Color hex code (e.g. #4F46E5) matching the map pin
    val track: String = "",    // e.g. "Keynote", "AI & ML", "Architecture"
    val description: String = "",
    val isBookmarked: Boolean = true
)
