package com.example

import com.example.data.model.ConferenceSession
import com.example.data.model.SessionJsonHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionJsonHelperTest {

    private fun row(extra: String = "", date: String = "2026-10-06", start: String = "09:00", end: String = "10:00") = """
        {"title":"Talk","speaker":"Ann","date":"$date","startTime":"$start","endTime":"$end",
         "locationName":"Hall A","latitude":37.78,"longitude":-122.4 $extra}
    """.trimIndent()

    @Test
    fun `round trip keeps track and every optional field`() {
        val original = ConferenceSession(
            title = "AI on the Edge", speaker = "Alice", speakerRole = "Lead",
            date = "2026-10-06", startTime = "09:30", endTime = "10:30",
            locationName = "Moscone", address = "747 Howard St",
            latitude = 37.783, longitude = -122.401, colorHex = "#4F46E5",
            track = "AI & ML", description = "Quantization deep dive"
        )
        val json = SessionJsonHelper.exportToJson(listOf(original))
        val imported = SessionJsonHelper.importFromJson(json).getOrThrow().single()

        assertEquals(original.copy(id = 0L), imported)
        assertTrue(json.contains("\"track\""))
    }

    @Test
    fun `json null is not imported as the text null`() {
        val json = "[" + row(""", "speakerRole": null, "address": null, "description": null, "track": null""") + "]"
        val session = SessionJsonHelper.importFromJson(json).getOrThrow().single()

        assertEquals("", session.speakerRole)
        assertEquals("", session.address)
        assertEquals("", session.description)
        assertEquals("", session.track)
    }

    @Test
    fun `invalid dates and times are skipped and reported`() {
        val json = "[" + listOf(
            row(),
            row(date = "2026-10-5x"),
            row(start = "25:00"),
            row(start = "11:00", end = "10:00"),
            row(date = "2026-02-30")
        ).joinToString(",") + "]"
        val report = SessionJsonHelper.parse(json).getOrThrow()

        assertEquals(1, report.sessions.size)
        assertEquals(4, report.skipped.size)
        assertTrue(report.skipped[0].startsWith("Item #2"))
    }

    @Test
    fun `single digit hours and short colors are normalised`() {
        val json = "[" + row(""", "colorHex": "#abc" """, start = "9:05", end = "9:45") + "]"
        val session = SessionJsonHelper.importFromJson(json).getOrThrow().single()

        assertEquals("09:05", session.startTime)
        assertEquals("09:45", session.endTime)
        assertEquals("#AABBCC", session.colorHex)
    }

    @Test
    fun `missing or out of range coordinates keep the session but mark it as unlocated`() {
        val noCoords = """{"title":"A","date":"2026-10-06","startTime":"09:00","endTime":"10:00"}"""
        val badCoords = row().replace("37.78", "95.0")
        val report = SessionJsonHelper.parse("[$noCoords,$badCoords]").getOrThrow()

        assertEquals(2, report.sessions.size)
        assertEquals(2, report.withoutLocation)
        assertFalse(report.sessions.any { it.hasLocation })
    }

    @Test
    fun `numeric strings are accepted for coordinates`() {
        val json = """[{"title":"A","date":"2026-10-06","startTime":"09:00","endTime":"10:00",
            "latitude":"37.5","longitude":"-122.25"}]"""
        val session = SessionJsonHelper.importFromJson(json).getOrThrow().single()

        assertEquals(37.5, session.latitude, 0.0)
        assertTrue(session.hasLocation)
    }

    @Test
    fun `byte order mark and single object root are accepted`() {
        val json = "\uFEFF" + row()
        assertEquals(1, SessionJsonHelper.importFromJson(json).getOrThrow().size)
    }

    @Test
    fun `sessions wrapper object is accepted`() {
        val json = """{"sessions": [${row()}]}"""
        assertEquals(1, SessionJsonHelper.importFromJson(json).getOrThrow().size)
    }

    @Test
    fun `garbage and empty input fail with a message`() {
        assertTrue(SessionJsonHelper.importFromJson("not json").isFailure)
        assertTrue(SessionJsonHelper.importFromJson("   ").isFailure)
        assertTrue(SessionJsonHelper.importFromJson("[]").isFailure)
        assertTrue(SessionJsonHelper.importFromJson("""{"foo": 1}""").isFailure)
    }

    @Test
    fun `strict date and time parsing`() {
        assertNull(SessionJsonHelper.parseDate("2026-13-01"))
        assertNull(SessionJsonHelper.parseTime("24:00"))
        assertEquals("2026-10-05", SessionJsonHelper.parseDate("2026-10-5").toString())
    }
}
