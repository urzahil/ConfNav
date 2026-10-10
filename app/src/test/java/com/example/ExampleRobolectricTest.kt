package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ConferenceSession
import com.example.ui.components.PinColorHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ConfNav", appName)
    }

    @Test
    fun `test pin color assignment is consistent`() {
        val color1 = PinColorHelper.getColorForLocation("Moscone Center South")
        val color2 = PinColorHelper.getColorForLocation("Moscone Center South")
        assertEquals(color1, color2)
        assertTrue(color1.startsWith("#"))
    }

    @Test
    fun `test color parsing`() {
        val parsed = PinColorHelper.parseColor("#4F46E5")
        assertNotNull(parsed)
    }

    @Test
    fun `test json export and import round trip`() {
        val originalSessions = listOf(
            ConferenceSession(
                id = 1L,
                title = "AI on the Edge",
                speaker = "Alice Wang",
                speakerRole = "AI Lead",
                date = "2026-10-06",
                startTime = "09:30",
                endTime = "10:30",
                locationName = "Moscone Center West",
                address = "747 Howard St",
                latitude = 37.783,
                longitude = -122.401,
                colorHex = "#4F46E5",
                description = "Deep dive into model quantization"
            ),
            ConferenceSession(
                id = 2L,
                title = "Compose Multiplatform",
                speaker = "Bob Martin",
                speakerRole = "Staff Engineer",
                date = "2026-10-07",
                startTime = "11:00",
                endTime = "12:00",
                locationName = "Yerba Buena Forum",
                address = "701 Mission St",
                latitude = 37.785,
                longitude = -122.403,
                colorHex = "#059669",
                description = "Sharing UI code across platforms"
            )
        )

        val exportedJson = com.example.data.model.SessionJsonHelper.exportToJson(originalSessions)
        assertTrue(exportedJson.contains("AI on the Edge"))
        assertTrue(exportedJson.contains("Compose Multiplatform"))
        assertTrue(exportedJson.contains("Moscone Center West"))

        val parseResult = com.example.data.model.SessionJsonHelper.importFromJson(exportedJson)
        assertTrue(parseResult.isSuccess)
        val imported = parseResult.getOrThrow()
        assertEquals(2, imported.size)
        assertEquals("AI on the Edge", imported[0].title)
        assertEquals("Alice Wang", imported[0].speaker)
        assertEquals("2026-10-06", imported[0].date)
        assertEquals("09:30", imported[0].startTime)
        assertEquals("Moscone Center West", imported[0].locationName)
        assertEquals("Compose Multiplatform", imported[1].title)
    }

    @Test
    fun `test json import with object wrapper`() {
        val wrappedJson = """
            {
              "sessions": [
                {
                  "title": "Quantum Computing 101",
                  "speaker": "Prof. Charles",
                  "date": "2026-10-08",
                  "startTime": "14:00",
                  "endTime": "15:00",
                  "locationName": "Metreon Lab",
                  "latitude": 37.784,
                  "longitude": -122.403
                }
              ]
            }
        """.trimIndent()

        val parseResult = com.example.data.model.SessionJsonHelper.importFromJson(wrappedJson)
        assertTrue(parseResult.isSuccess)
        val imported = parseResult.getOrThrow()
        assertEquals(1, imported.size)
        assertEquals("Quantum Computing 101", imported[0].title)
        assertEquals("Prof. Charles", imported[0].speaker)
        assertEquals("2026-10-08", imported[0].date)
    }

    @Test
    fun `test json import invalid fails gracefully`() {
        val invalidJson = "This is not valid json content"
        val parseResult = com.example.data.model.SessionJsonHelper.importFromJson(invalidJson)
        assertTrue(parseResult.isFailure)
    }
}
