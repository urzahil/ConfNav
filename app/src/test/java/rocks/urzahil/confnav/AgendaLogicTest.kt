package rocks.urzahil.confnav

import rocks.urzahil.confnav.data.model.ConferenceSession
import rocks.urzahil.confnav.ui.AgendaLogic
import rocks.urzahil.confnav.ui.DayFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class AgendaLogicTest {

    private fun session(id: Long, date: String, start: String, end: String, venue: String = "Hall") =
        ConferenceSession(
            id = id, title = "S$id", speaker = "x", date = date, startTime = start, endTime = end,
            locationName = venue, address = "", latitude = 1.0, longitude = 1.0, colorHex = "#4F46E5"
        )

    private val noon = LocalDateTime.of(2026, 10, 5, 12, 0)

    @Test
    fun `session ending now is still ongoing and one minute later is over`() {
        val s = session(1, "2026-10-05", "11:00", "12:00")
        assertTrue(AgendaLogic.isOngoingOrUpcoming(s, noon))
        assertTrue(AgendaLogic.isOngoingOrUpcoming(s, noon.withSecond(30)))
        assertFalse(AgendaLogic.isOngoingOrUpcoming(s, noon.plusMinutes(1)))
    }

    @Test
    fun `next session moves on as time passes`() {
        val morning = session(1, "2026-10-05", "09:00", "10:30")
        val afternoon = session(2, "2026-10-05", "14:00", "15:00")
        val tomorrow = session(3, "2026-10-06", "09:00", "10:00")
        val all = listOf(tomorrow, afternoon, morning)

        assertEquals(1L, AgendaLogic.nextSession(all, noon.withHour(10).withMinute(0))?.id)
        assertEquals(2L, AgendaLogic.nextSession(all, noon)?.id)
        assertEquals(3L, AgendaLogic.nextSession(all, noon.withHour(16))?.id)
        assertNull(AgendaLogic.nextSession(all, noon.plusDays(2)))
    }

    @Test
    fun `sessions with unreadable times are never upcoming`() {
        assertFalse(AgendaLogic.isOngoingOrUpcoming(session(1, "soon", "09:00", "10:00"), noon))
    }

    @Test
    fun `duplicate key ignores case and surrounding spaces`() {
        val a = session(1, "2026-10-05", "09:00", "10:00", "Main Hall")
        val b = a.copy(id = 9, title = " s1 ", locationName = "main hall ")
        assertEquals(AgendaLogic.duplicateKey(a), AgendaLogic.duplicateKey(b))
    }

    @Test
    fun `calendar layout puts overlapping sessions in separate columns and reuses freed ones`() {
        val long = session(1, "2026-10-05", "13:00", "17:00")
        val first = session(2, "2026-10-05", "13:00", "14:00")
        val next = session(3, "2026-10-05", "14:00", "15:30")
        val overlap = session(4, "2026-10-05", "15:15", "16:15")
        val evening = session(5, "2026-10-05", "20:00", "21:00")

        val columns = AgendaLogic.layoutCalendarDay(listOf(evening, overlap, next, first, long))
            .associate { it.session.id to it.column }

        assertEquals(mapOf(1L to 0, 2L to 1, 3L to 1, 4L to 2, 5L to 0), columns)
    }

    @Test
    fun `starred sessions are placed past every session they overlap`() {
        val workshop = session(1, "2026-10-05", "09:00", "12:00")
        val starred = session(2, "2026-10-05", "10:00", "11:00").copy(starred = true)
        val parallel = session(3, "2026-10-05", "10:30", "11:30")
        val later = session(4, "2026-10-05", "13:00", "14:00").copy(starred = true)

        val columns = AgendaLogic.layoutCalendarDay(listOf(workshop, starred, parallel, later))
            .associate { it.session.id to it.column }

        assertEquals(mapOf(1L to 0, 2L to 2, 3L to 1, 4L to 0), columns)
    }

    @Test
    fun `starred only filter combines with the day filter`() {
        val day1 = session(1, "2026-10-05", "09:00", "10:00").copy(starred = true)
        val day1Plain = session(2, "2026-10-05", "11:00", "12:00")
        val day2 = session(3, "2026-10-06", "09:00", "10:00").copy(starred = true)
        val all = listOf(day1, day1Plain, day2)
        val oct5 = DayFilter.SpecificDate("2026-10-05", "Oct 5")

        fun ids(filter: DayFilter, starredOnly: Boolean) =
            AgendaLogic.filterSessions(all, filter, starredOnly).map { it.id }

        assertEquals(listOf(1L, 2L, 3L), ids(DayFilter.All, false))
        assertEquals(listOf(1L, 3L), ids(DayFilter.All, true))
        assertEquals(listOf(1L, 2L), ids(oct5, false))
        assertEquals(listOf(1L), ids(oct5, true))
    }

    @Test
    fun `calendar layout stretches short sessions and skips unreadable times`() {
        val short = session(1, "2026-10-05", "14:00", "14:00")
        val broken = session(2, "2026-10-05", "later", "15:00")

        val slots = AgendaLogic.layoutCalendarDay(listOf(short, broken), minDurationMinutes = 30)

        assertEquals(1, slots.size)
        assertEquals(14 * 60, slots[0].startMinute)
        assertEquals(14 * 60 + 30, slots[0].endMinute)
    }
}
