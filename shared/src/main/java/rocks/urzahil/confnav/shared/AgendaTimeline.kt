package rocks.urzahil.confnav.shared

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.time.temporal.ChronoUnit

/** A stretch of time during which the watch shows [session], either as on now or as up next. */
data class TimelineSlot(
    val start: LocalDateTime,
    val end: LocalDateTime,
    val session: WatchSession,
    val ongoing: Boolean
)

/**
 * Works out what the watch shows and when it changes. The "next session" rule is the phone's
 * (`AgendaLogic.nextSession`): the ongoing session, or otherwise the soonest upcoming one, where
 * a session counts until the end of its last minute. Because the whole day is known up front, the
 * tile and complication can be handed every change in advance instead of waking up to recompute.
 */
object AgendaTimeline {

    private val DATE_FORMAT = DateTimeFormatter.ofPattern("uuuu-M-d").withResolverStyle(ResolverStyle.STRICT)
    private val TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm").withResolverStyle(ResolverStyle.STRICT)

    fun startOf(session: WatchSession): LocalDateTime? =
        parse(session.date, session.startTime)

    fun endOf(session: WatchSession): LocalDateTime? =
        parse(session.date, session.endTime)

    /** The ongoing session, or otherwise the soonest upcoming one; null when nothing is left. */
    fun nextSession(sessions: List<WatchSession>, now: LocalDateTime): WatchSession? {
        val minute = now.truncatedTo(ChronoUnit.MINUTES)
        return sessions
            .filter { session -> endOf(session)?.isBefore(minute) == false }
            .minByOrNull { startOf(it) ?: LocalDateTime.MAX }
    }

    /**
     * The slots from [now] until the last session ends, in order, with at most [maxSlots] of
     * them. A new slot begins whenever the shown session changes or it goes from up next to on
     * now. Empty when nothing is left.
     */
    fun build(sessions: List<WatchSession>, now: LocalDateTime, maxSlots: Int = 24): List<TimelineSlot> {
        // The answer can only change when a session starts or once its last minute is over.
        val changes = sessions
            .flatMap { listOfNotNull(startOf(it), endOf(it)?.plusMinutes(1)) }
            .filter { it.isAfter(now) }
            .toSortedSet()
        val boundaries = listOf(now) + changes

        val slots = mutableListOf<TimelineSlot>()
        for ((from, to) in boundaries.zipWithNext()) {
            val session = nextSession(sessions, from) ?: break
            val ongoing = startOf(session)?.isAfter(from) == false
            val previous = slots.lastOrNull()
            if (previous != null && previous.session == session && previous.ongoing == ongoing) {
                slots[slots.lastIndex] = previous.copy(end = to)
            } else {
                if (slots.size == maxSlots) break
                slots += TimelineSlot(from, to, session, ongoing)
            }
        }
        return slots
    }

    private fun parse(date: String, time: String): LocalDateTime? = try {
        LocalDate.parse(date.trim(), DATE_FORMAT).atTime(LocalTime.parse(time.trim(), TIME_FORMAT))
    } catch (_: Exception) {
        null
    }
}
