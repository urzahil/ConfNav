package com.example.ui

import com.example.data.model.ConferenceSession
import com.example.data.model.SessionJsonHelper
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Pure, clock-independent agenda rules. Every function takes `now` explicitly so the
 * behaviour can be unit tested and so the view model can refresh it as time passes.
 */
object AgendaLogic {

    /** When the session ends, or null if its stored date/time cannot be parsed. */
    fun endOf(session: ConferenceSession): LocalDateTime? {
        val date = SessionJsonHelper.parseDate(session.date) ?: return null
        val end = SessionJsonHelper.parseTime(session.endTime) ?: return null
        return date.atTime(end)
    }

    /** When the session starts, or null if its stored date/time cannot be parsed. */
    fun startOf(session: ConferenceSession): LocalDateTime? {
        val date = SessionJsonHelper.parseDate(session.date) ?: return null
        val start = SessionJsonHelper.parseTime(session.startTime) ?: return null
        return date.atTime(start)
    }

    /** True while the session is ongoing or still to come. Sessions with unreadable times are not. */
    fun isOngoingOrUpcoming(session: ConferenceSession, now: LocalDateTime): Boolean {
        val end = endOf(session) ?: return false
        return !end.isBefore(now.truncatedTo(ChronoUnit.MINUTES))
    }

    /** The ongoing session, or otherwise the soonest upcoming one; null when nothing is left. */
    fun nextSession(sessions: List<ConferenceSession>, now: LocalDateTime): ConferenceSession? =
        sessions
            .filter { isOngoingOrUpcoming(it, now) }
            .minByOrNull { startOf(it) ?: LocalDateTime.MAX }

    /** Identity used to avoid re-adding the same session when importing on top of an agenda. */
    fun duplicateKey(session: ConferenceSession): String =
        listOf(
            session.title.trim().lowercase(),
            session.date.trim(),
            session.startTime.trim(),
            session.locationName.trim().lowercase()
        ).joinToString("|")
}
