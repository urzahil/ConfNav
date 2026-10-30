package rocks.urzahil.confnav.wear

import android.app.PendingIntent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataTimeline
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingTimelineComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.TimeInterval
import androidx.wear.watchface.complications.datasource.TimelineEntry
import rocks.urzahil.confnav.shared.AgendaTimeline
import rocks.urzahil.confnav.shared.TimelineSlot
import rocks.urzahil.confnav.shared.WatchSession
import java.time.Instant
import java.time.LocalDateTime

/**
 * Watch-face complication for the next session: "09:30 · Main Hall" (or "NOW") in a short slot,
 * the session title in a long one. The watch face decides how it looks; tapping it opens
 * directions. Like the tile, it hands over the rest of the agenda as a timeline.
 */
class NextEventComplicationService : SuspendingTimelineComplicationDataSourceService() {

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationDataTimeline? {
        val sessions = AgendaStore.load(this).orEmpty()
        val slots = AgendaTimeline.build(sessions, LocalDateTime.now(), NextEventTileService.MAX_SLOTS)
        return ComplicationDataTimeline(
            defaultComplicationData = NoDataComplicationData(),
            timelineEntries = slots.mapNotNull { slot ->
                val data = complicationData(request.complicationType, slot) ?: return@mapNotNull null
                TimelineEntry(
                    TimeInterval(
                        Instant.ofEpochMilli(slot.start.toEpochMillis()),
                        Instant.ofEpochMilli(slot.end.toEpochMillis())
                    ),
                    data
                )
            }
        )
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val start = LocalDateTime.of(2026, 10, 20, 8, 30)
        val session = WatchSession(
            title = "Opening Keynote", speaker = "Ada Lovelace", date = "2026-10-20",
            startTime = "09:00", endTime = "10:00", locationName = "Main Hall",
            latitude = 40.7128, longitude = -74.006, colorHex = "#0D9488"
        )
        return complicationData(type, TimelineSlot(start, start.plusMinutes(30), session, ongoing = false))
    }

    private fun complicationData(type: ComplicationType, slot: TimelineSlot): ComplicationData? {
        val session = slot.session
        val time = if (slot.ongoing) "NOW" else session.startTime
        val description = plainText(
            "${if (slot.ongoing) "Now" else "Up next at ${session.startTime}"}: ${session.title}, ${session.locationName}"
        )
        val pin = MonochromaticImage.Builder(Icon.createWithResource(this, R.drawable.ic_location)).build()

        return when (type) {
            ComplicationType.SHORT_TEXT ->
                ShortTextComplicationData.Builder(plainText(time), description)
                    .setTitle(plainText(session.locationName))
                    .setMonochromaticImage(pin)
                    .setTapAction(directionsIntent(session))
                    .build()

            ComplicationType.LONG_TEXT ->
                LongTextComplicationData.Builder(plainText(session.title), description)
                    .setTitle(plainText("$time · ${session.locationName}"))
                    .setMonochromaticImage(pin)
                    .setTapAction(directionsIntent(session))
                    .build()

            else -> null
        }
    }

    private fun directionsIntent(session: WatchSession): PendingIntent? {
        if (!session.hasLocation) return null
        return PendingIntent.getActivity(
            this,
            // One request code per session so each slot keeps its own destination.
            session.hashCode(),
            DirectionsActivity.intent(this, session),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun plainText(text: String) = PlainComplicationText.Builder(text).build()
}
