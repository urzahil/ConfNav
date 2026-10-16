package com.example

import com.example.data.model.ConferenceSession
import com.example.ui.AgendaLogic
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
}
