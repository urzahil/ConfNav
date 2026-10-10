package com.example.data.repository

import com.example.data.local.ConferenceDao
import com.example.data.model.ConferenceSession
import com.example.data.model.LocationGroup
import com.example.ui.components.PinColorHelper
import kotlinx.coroutines.flow.Flow

class ConferenceRepository(private val dao: ConferenceDao) {

    val allSessions: Flow<List<ConferenceSession>> = dao.getAllSessions()

    suspend fun insertSession(session: ConferenceSession): Long = dao.insertSession(session)

    suspend fun insertSessions(sessions: List<ConferenceSession>) = dao.insertSessions(sessions)

    suspend fun updateSession(session: ConferenceSession) = dao.updateSession(session)

    suspend fun deleteSessionById(id: Long) = dao.deleteSessionById(id)

    suspend fun clearAll() = dao.clearAll()

    /**
     * Remove any leftover sample demo sessions from previous template builds
     * so the app starts completely clean.
     */
    suspend fun clearSampleEventsIfAny() {
        val sampleTitles = setOf(
            "Opening Keynote: The Next Frontier in Tech",
            "Modern Android & Jetpack Compose Masterclass",
            "Designing Spatial & Ambient User Interfaces",
            "Edge Computing & Low-Latency Geocoding",
            "Fireside Chat: AI Ethics & Systems Architecture",
            "Conference Hackathon Showcase & Demos",
            "Closing Keynote & Awards Ceremony"
        )
        // Remove only the known template sessions. Never clear user-created sessions.
        try {
            dao.deleteByTitles(sampleTitles.toList())
        } catch (_: Exception) {
            // Cleanup is best-effort; failure must never affect the user's agenda.
        }
    }

    /**
     * Group sessions by unique location (matching lat/lng or location name).
     * If multiple events share the same location, they are grouped under one pin.
     */
    fun groupSessionsByLocation(sessions: List<ConferenceSession>): List<LocationGroup> {
        val grouped = LinkedHashMap<String, MutableList<ConferenceSession>>()

        for (session in sessions) {
            // Group key based on location name and coordinates
            val key = "${session.locationName.trim().lowercase()}_${
                String.format(
                    java.util.Locale.US,
                    "%.4f,%.4f",
                    session.latitude,
                    session.longitude
                )
            }"
            grouped.getOrPut(key) { mutableListOf() }.add(session)
        }

        var index = 0
        return grouped.map { (key, sessionList) ->
            val first = sessionList.first()
            val color = if (first.colorHex.isNotBlank() && first.colorHex.startsWith("#")) {
                first.colorHex
            } else {
                PinColorHelper.getColorForLocation(first.locationName, index)
            }
            index++

            LocationGroup(
                key = key,
                locationName = first.locationName,
                address = first.address,
                latitude = first.latitude,
                longitude = first.longitude,
                colorHex = color,
                sessions = sessionList.sortedBy { "${it.date} ${it.startTime}" }
            )
        }
    }
}
