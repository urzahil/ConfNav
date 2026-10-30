package rocks.urzahil.confnav.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class AgendaTimelineTest {

    private fun session(title: String, date: String, start: String, end: String) =
        WatchSession(
            title = title, speaker = "x", date = date, startTime = start, endTime = end,
            locationName = "Hall", latitude = 1.0, longitude = 1.0, colorHex = "#4F46E5"
        )

    private fun at(hour: Int, minute: Int = 0, day: Int = 5) = LocalDateTime.of(2026, 10, day, hour, minute)

    private val morning = session("Morning", "2026-10-05", "09:00", "10:30")
    private val afternoon = session("Afternoon", "2026-10-05", "14:00", "15:00")
    private val tomorrow = session("Tomorrow", "2026-10-06", "09:00", "10:00")

    @Test
    fun `next session matches the phone rule`() {
        val all = listOf(tomorrow, afternoon, morning)

        assertEquals("Morning", AgendaTimeline.nextSession(all, at(10, 30).withSecond(59))?.title)
        assertEquals("Afternoon", AgendaTimeline.nextSession(all, at(10, 31))?.title)
        assertEquals("Tomorrow", AgendaTimeline.nextSession(all, at(16))?.title)
        assertNull(AgendaTimeline.nextSession(all, at(12, day = 7)))
    }

    @Test
    fun `timeline covers the day with a slot per change`() {
        val slots = AgendaTimeline.build(listOf(tomorrow, afternoon, morning), at(8))

        assertEquals(
            listOf(
                Triple("Morning", false, at(8) to at(9)),
                Triple("Morning", true, at(9) to at(10, 31)),
                Triple("Afternoon", false, at(10, 31) to at(14)),
                Triple("Afternoon", true, at(14) to at(15, 1)),
                Triple("Tomorrow", false, at(15, 1) to at(9, day = 6)),
                Triple("Tomorrow", true, at(9, day = 6) to at(10, 1, day = 6))
            ),
            slots.map { Triple(it.session.title, it.ongoing, it.start to it.end) }
        )
    }

    @Test
    fun `timeline starts mid-session as on now`() {
        val slots = AgendaTimeline.build(listOf(morning), at(9, 15))

        assertEquals(1, slots.size)
        assertTrue(slots[0].ongoing)
        assertEquals(at(9, 15), slots[0].start)
        assertEquals(at(10, 31), slots[0].end)
    }

    @Test
    fun `an overlapping later session waits until the earlier one is over`() {
        val keynote = session("Keynote", "2026-10-05", "09:00", "10:00")
        val workshop = session("Workshop", "2026-10-05", "09:30", "11:00")

        val slots = AgendaTimeline.build(listOf(workshop, keynote), at(9))

        assertEquals(
            listOf("Keynote" to (at(9) to at(10, 1)), "Workshop" to (at(10, 1) to at(11, 1))),
            slots.map { it.session.title to (it.start to it.end) }
        )
        assertTrue(slots.all { it.ongoing })
    }

    @Test
    fun `nothing left gives an empty timeline`() {
        assertTrue(AgendaTimeline.build(listOf(morning), at(11)).isEmpty())
        assertTrue(AgendaTimeline.build(emptyList(), at(11)).isEmpty())
    }

    @Test
    fun `timeline is capped`() {
        val many = (8..20).map { hour -> session("S$hour", "2026-10-05", "$hour:00", "$hour:30") }

        val slots = AgendaTimeline.build(many, at(7), maxSlots = 5)

        assertEquals(5, slots.size)
        assertEquals(listOf("S8", "S8", "S9", "S9", "S10"), slots.map { it.session.title })
    }

    @Test
    fun `sessions with unreadable times are left out`() {
        val broken = session("Broken", "2026-13-40", "09:00", "10:00")

        assertTrue(AgendaTimeline.build(listOf(broken), at(8)).isEmpty())
        assertFalse(broken.copy(latitude = 0.0, longitude = 0.0).hasLocation)
    }
}
