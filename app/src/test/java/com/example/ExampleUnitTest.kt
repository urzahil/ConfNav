package com.example

import com.example.data.model.ConferenceSession
import com.example.data.model.LocationGroup
import com.example.ui.components.PinColorHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testLocationGroupSessionCount() {
        val session1 = ConferenceSession(
            id = 1L,
            title = "Keynote",
            speaker = "Dr. Vance",
            date = "2026-10-05",
            startTime = "09:00",
            endTime = "10:30",
            locationName = "Main Hall",
            address = "123 Conf Way",
            latitude = 37.784,
            longitude = -122.401,
            colorHex = "#4F46E5"
        )
        val session2 = ConferenceSession(
            id = 2L,
            title = "Deep Dive",
            speaker = "Marcus Chen",
            date = "2026-10-05",
            startTime = "11:00",
            endTime = "12:00",
            locationName = "Main Hall",
            address = "123 Conf Way",
            latitude = 37.784,
            longitude = -122.401,
            colorHex = "#4F46E5"
        )

        val group = LocationGroup(
            key = "main_hall",
            locationName = "Main Hall",
            address = "123 Conf Way",
            latitude = 37.784,
            longitude = -122.401,
            colorHex = "#4F46E5",
            sessions = listOf(session1, session2)
        )

        assertEquals(2, group.sessionCount)
        assertTrue(group.subtitle.contains("2 sessions"))
    }

    @Test
    fun testMultiDayDynamicHandling() {
        // Test that sessions spanning 5 days are correctly recognized
        val dates = listOf("2026-10-05", "2026-10-06", "2026-10-07", "2026-10-08", "2026-10-09")
        val sessions = dates.mapIndexed { index, date ->
            ConferenceSession(
                id = index + 1L,
                title = "Session on Day ${index + 1}",
                speaker = "Speaker $index",
                date = date,
                startTime = "10:00",
                endTime = "11:00",
                locationName = "Room $index",
                address = "Address $index",
                latitude = 37.78 + index * 0.001,
                longitude = -122.40 - index * 0.001,
                colorHex = PinColorHelper.PALETTE[index % PinColorHelper.PALETTE.size]
            )
        }

        val uniqueDates = sessions.map { it.date }.distinct().sorted()
        assertEquals(5, uniqueDates.size)
        assertEquals("2026-10-05", uniqueDates.first())
        assertEquals("2026-10-09", uniqueDates.last())
    }

    @Test
    fun testFutureEventsFiltering() {
        val todayStr = "2026-10-05"
        val nowTime = "12:00"

        val pastSession = ConferenceSession(
            id = 1L,
            title = "Past Keynote",
            speaker = "Speaker 1",
            date = "2026-10-05",
            startTime = "09:00",
            endTime = "10:30",
            locationName = "Hall A",
            address = "Addr A",
            latitude = 37.78,
            longitude = -122.40,
            colorHex = "#4F46E5"
        )
        val futureTodaySession = ConferenceSession(
            id = 2L,
            title = "Afternoon Session",
            speaker = "Speaker 2",
            date = "2026-10-05",
            startTime = "14:00",
            endTime = "15:30",
            locationName = "Hall B",
            address = "Addr B",
            latitude = 37.79,
            longitude = -122.41,
            colorHex = "#059669"
        )
        val tomorrowSession = ConferenceSession(
            id = 3L,
            title = "Tomorrow Keynote",
            speaker = "Speaker 3",
            date = "2026-10-06",
            startTime = "10:00",
            endTime = "11:00",
            locationName = "Hall C",
            address = "Addr C",
            latitude = 37.80,
            longitude = -122.42,
            colorHex = "#D97706"
        )

        val allSessions = listOf(pastSession, futureTodaySession, tomorrowSession)
        val futureOnly = allSessions.filter {
            it.date > todayStr || (it.date == todayStr && it.endTime >= nowTime)
        }

        assertEquals(2, futureOnly.size)
        assertTrue(futureOnly.none { it.id == pastSession.id })
        assertTrue(futureOnly.any { it.id == futureTodaySession.id })
        assertTrue(futureOnly.any { it.id == tomorrowSession.id })
    }

    @Test
    fun testVenueColorReusedForSameVenue() {
        val existingSession = ConferenceSession(
            id = 10L,
            title = "Morning Workshop",
            speaker = "Dr. Alice",
            date = "2026-10-05",
            startTime = "09:00",
            endTime = "10:00",
            locationName = "Moscone Center West",
            address = "747 Howard St",
            latitude = 37.783,
            longitude = -122.401,
            colorHex = "#4F46E5"
        )

        val sessions = listOf(existingSession)
        val cleanName = "Moscone Center West".trim()

        val matchingSession = sessions.firstOrNull {
            it.locationName.trim().equals(cleanName, ignoreCase = true)
        }

        // When adding another session at the same venue, it must reuse the exact same color
        assertEquals("#4F46E5", matchingSession?.colorHex)
    }

    @Test
    fun testVenueColorAutoPicksUnusedColorForNewVenue() {
        val usedColors = setOf("#4F46E5", "#059669")
        val unusedColor = PinColorHelper.getUnusedOrDistinctColor(usedColors, "Metreon")

        assertTrue(unusedColor !in usedColors)
    }

    @Test
    fun testNeverReusesColorEvenIfVenueHasNoFutureEvents() {
        val todayStr = "2026-10-06"
        val nowTime = "18:00"

        // Venue A only has an old event in the past (no future events at Venue A)
        val pastSessionAtVenueA = ConferenceSession(
            id = 101L,
            title = "Past Opening Speech",
            speaker = "Dr. Old",
            date = "2026-10-05",
            startTime = "08:00",
            endTime = "09:00",
            locationName = "Old Hall A",
            address = "123 Old St",
            latitude = 37.781,
            longitude = -122.401,
            colorHex = "#4F46E5"
        )

        // Venue B has a future event
        val futureSessionAtVenueB = ConferenceSession(
            id = 102L,
            title = "Upcoming Speech",
            speaker = "Dr. Future",
            date = "2026-10-07",
            startTime = "10:00",
            endTime = "11:00",
            locationName = "Future Hall B",
            address = "456 Future St",
            latitude = 37.785,
            longitude = -122.405,
            colorHex = "#059669"
        )

        val allSessions = listOf(pastSessionAtVenueA, futureSessionAtVenueB)

        // Verify that Old Hall A has NO future events
        val futureEventsAtVenueA = allSessions.filter {
            it.locationName == "Old Hall A" &&
                    (it.date > todayStr || (it.date == todayStr && it.endTime >= nowTime))
        }
        assertEquals(0, futureEventsAtVenueA.size)

        // All used colors across all sessions in the DB (including past events with no future events)
        val allUsedColors = allSessions.map { it.colorHex.uppercase() }.toSet()
        assertTrue(allUsedColors.contains("#4F46E5"))
        assertTrue(allUsedColors.contains("#059669"))

        // Pick color for a brand new venue C: must NOT pick #4F46E5 or #059669
        val newVenueColor =
            PinColorHelper.getUnusedOrDistinctColor(allUsedColors, "Brand New Venue C")

        assertTrue(
            "New color must not reuse past venue color",
            newVenueColor.uppercase() != "#4F46E5"
        )
        assertTrue(
            "New color must not reuse future venue color",
            newVenueColor.uppercase() != "#059669"
        )
        assertTrue(
            "New color must not be in usedColors",
            newVenueColor.uppercase() !in allUsedColors
        )
    }

    @Test
    fun testJsonExportAndImportRoundTrip() {
        val originalSession = ConferenceSession(
            id = 1L,
            title = "Keynote Address",
            speaker = "Dr. Elena Vance",
            speakerRole = "Chief Scientist",
            date = "2026-10-05",
            startTime = "09:00",
            endTime = "10:15",
            locationName = "Moscone Center South",
            address = "747 Howard St, San Francisco, CA",
            latitude = 37.784,
            longitude = -122.401,
            colorHex = "#4F46E5",
            description = "Welcome and key conference highlights."
        )

        val exportedJson =
            com.example.data.model.SessionJsonHelper.exportToJson(listOf(originalSession))
        assertTrue(exportedJson.contains("Keynote Address"))
        assertTrue(exportedJson.contains("Moscone Center South"))

        val importResult = com.example.data.model.SessionJsonHelper.importFromJson(exportedJson)
        assertTrue(importResult.isSuccess)

        val importedSessions = importResult.getOrThrow()
        assertEquals(1, importedSessions.size)
        val imported = importedSessions.first()
        assertEquals("Keynote Address", imported.title)
        assertEquals("Dr. Elena Vance", imported.speaker)
        assertEquals("Moscone Center South", imported.locationName)
        assertEquals("747 Howard St, San Francisco, CA", imported.address)
        assertEquals("2026-10-05", imported.date)
        assertEquals("09:00", imported.startTime)
        assertEquals("10:15", imported.endTime)
    }
}
