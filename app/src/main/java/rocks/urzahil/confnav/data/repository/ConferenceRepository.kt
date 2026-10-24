package rocks.urzahil.confnav.data.repository

import rocks.urzahil.confnav.data.local.ConferenceDao
import rocks.urzahil.confnav.data.model.ConferenceSession
import rocks.urzahil.confnav.data.model.LocationGroup
import rocks.urzahil.confnav.ui.components.PinColorHelper
import kotlinx.coroutines.flow.Flow

class ConferenceRepository(private val dao: ConferenceDao) {

    val allSessions: Flow<List<ConferenceSession>> = dao.getAllSessions()

    suspend fun insertSession(session: ConferenceSession): Long = dao.insertSession(session)

    suspend fun insertSessions(sessions: List<ConferenceSession>) = dao.insertSessions(sessions)

    /** Atomically swaps the whole agenda for [sessions]. */
    suspend fun replaceAll(sessions: List<ConferenceSession>) = dao.replaceAll(sessions)

    suspend fun updateSession(session: ConferenceSession) = dao.updateSession(session)

    suspend fun deleteSessionById(id: Long) = dao.deleteSessionById(id)

    /**
     * Group sessions into map pins, one per venue. Sessions without usable coordinates are
     * skipped. Sessions share a pin when they have the same venue name (ignoring case and
     * extra spaces), so small coordinate differences between rows do not create duplicate pins.
     * A session with no venue name is grouped by its coordinates instead.
     */
    fun groupSessionsByLocation(sessions: List<ConferenceSession>): List<LocationGroup> {
        val grouped = LinkedHashMap<String, MutableList<ConferenceSession>>()

        for (session in sessions) {
            if (!session.hasLocation) continue
            grouped.getOrPut(locationKey(session)) { mutableListOf() }.add(session)
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

    private fun locationKey(session: ConferenceSession): String {
        val name = session.locationName.trim().lowercase().replace(Regex("\\s+"), " ")
        return if (name.isNotEmpty()) {
            name
        } else {
            String.format(java.util.Locale.US, "%.4f,%.4f", session.latitude, session.longitude)
        }
    }
}
