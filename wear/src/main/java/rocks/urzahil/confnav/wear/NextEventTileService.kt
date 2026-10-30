package rocks.urzahil.confnav.wear

import android.content.Context
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.TimelineBuilders.TimelineEntry
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.materialScopeWithResources
import androidx.wear.tiles.Material3TileService
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.timeInterval
import androidx.wear.tiles.timeline
import androidx.wear.tiles.timelineEntry
import androidx.wear.tiles.tile
import androidx.wear.tiles.tooling.preview.Preview
import androidx.wear.tiles.tooling.preview.TilePreviewData
import androidx.wear.tooling.preview.devices.WearDevices
import rocks.urzahil.confnav.shared.AgendaTimeline
import rocks.urzahil.confnav.shared.TimelineSlot
import rocks.urzahil.confnav.shared.WatchSession
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

/**
 * The "next event" tile. Every change for the rest of the agenda is handed over up front as a
 * timeline, so the tile switches to the next session on time without waking the app. The phone
 * pushing a new agenda triggers a fresh tile (see [AgendaListenerService]).
 */
class NextEventTileService : Material3TileService(
    // Keep ConfNav's own colours rather than the watch's dynamic theme.
    allowDynamicTheme = false,
    defaultColorScheme = ConfNavWearTheme.colorScheme
) {
    override suspend fun MaterialScope.tileResponse(requestParams: TileRequest): Tile {
        val sessions = AgendaStore.load(this@NextEventTileService)
        val slots = AgendaTimeline.build(sessions.orEmpty(), LocalDateTime.now(), MAX_SLOTS)
        return tile(
            timeline = nextEventTimeline(slots, hasAgenda = sessions != null),
            // The timeline is exact; this only catches time zone changes and capped timelines.
            freshness = 1.hours
        )
    }

    companion object {
        /** Enough for a full conference day while keeping the tile payload small. */
        const val MAX_SLOTS = 24
    }
}

/** One entry per slot, plus an entry with no validity that shows once every slot is over. */
private fun MaterialScope.nextEventTimeline(slots: List<TimelineSlot>, hasAgenda: Boolean) =
    timeline(
        *slots.map { slot ->
            timelineEntry(
                layout = nextEventLayout(slot),
                validity = timeInterval(slot.start.toEpochMillis().milliseconds, slot.end.toEpochMillis().milliseconds)
            )
        }.toTypedArray<TimelineEntry>(),
        timelineEntry(noEventLayout(hasAgenda))
    )

internal fun LocalDateTime.toEpochMillis(): Long = atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

private val previewSession = WatchSession(
    title = "Opening Keynote",
    speaker = "Ada Lovelace",
    date = "2026-10-20",
    startTime = "09:00",
    endTime = "10:00",
    locationName = "Main Hall",
    latitude = 40.7128,
    longitude = -74.006,
    colorHex = "#0D9488",
    starred = true
)

private fun previewTile(context: Context, layout: MaterialScope.() -> LayoutElement) =
    TilePreviewData { request ->
        tile(
            timeline(
                timelineEntry(
                    materialScopeWithResources(
                        context = context,
                        protoLayoutScope = request.scope,
                        deviceConfiguration = request.deviceConfiguration,
                        allowDynamicTheme = false,
                        defaultColorScheme = ConfNavWearTheme.colorScheme,
                        layout = layout
                    )
                )
            )
        )
    }

@Preview(device = WearDevices.SMALL_ROUND)
@Preview(device = WearDevices.LARGE_ROUND)
fun upNextPreview(context: Context) = previewTile(context) {
    val start = LocalDateTime.of(2026, 10, 20, 8, 30)
    nextEventLayout(TimelineSlot(start, start.plusMinutes(30), previewSession, ongoing = false))
}

@Preview(device = WearDevices.SMALL_ROUND)
fun noEventPreview(context: Context) = previewTile(context) { noEventLayout(hasAgenda = true) }
