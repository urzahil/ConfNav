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
) {
    /**
     * False when the session has no usable coordinates (0,0 is the "not set" value).
     * Such sessions stay in the agenda but are left off the map and have no directions.
     */
    val hasLocation: Boolean
        get() = latitude.isFinite() && longitude.isFinite() &&
                latitude in -90.0..90.0 && longitude in -180.0..180.0 &&
                !(latitude == 0.0 && longitude == 0.0)
}
