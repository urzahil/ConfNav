package rocks.urzahil.confnav.ui

import rocks.urzahil.confnav.data.model.ConferenceSession
import rocks.urzahil.confnav.data.model.SessionJsonHelper
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/** A session placed on the calendar day grid: minutes since midnight plus its overlap column. */
data class CalendarSlot(
    val session: ConferenceSession,
    val startMinute: Int,
    val endMinute: Int,
    val column: Int
)

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

    /**
     * Lays out one day's sessions for the calendar grid, Google-Calendar style: sessions that
     * overlap are put in separate columns (reusing a column once it is free again), so each
     * can be drawn indented over the earlier ones. Sessions shorter than [minDurationMinutes]
     * are stretched so they stay readable; sessions with unreadable times are left out.
     */
    fun layoutCalendarDay(
        sessions: List<ConferenceSession>,
        minDurationMinutes: Int = 30
    ): List<CalendarSlot> {
        val timed = sessions.mapNotNull { session ->
            val start = SessionJsonHelper.parseTime(session.startTime) ?: return@mapNotNull null
            val end = SessionJsonHelper.parseTime(session.endTime) ?: return@mapNotNull null
            val startMinute = start.hour * 60 + start.minute
            val endMinute = maxOf(end.hour * 60 + end.minute, startMinute + minDurationMinutes)
            Triple(session, startMinute, endMinute)
        }.sortedWith(compareBy({ it.second }, { -it.third }))

        val slots = mutableListOf<CalendarSlot>()
        val columnEnds = mutableListOf<Int>()
        var clusterEnd = -1
        for ((session, start, end) in timed) {
            if (start >= clusterEnd) {
                // Nothing still running overlaps this one: start a fresh set of columns.
                columnEnds.clear()
                clusterEnd = end
            }
            var column = columnEnds.indexOfFirst { it <= start }
            if (column == -1) {
                column = columnEnds.size
                columnEnds.add(end)
            } else {
                columnEnds[column] = end
            }
            clusterEnd = maxOf(clusterEnd, end)
            slots.add(CalendarSlot(session, start, end, column))
        }
        return slots
    }
}
