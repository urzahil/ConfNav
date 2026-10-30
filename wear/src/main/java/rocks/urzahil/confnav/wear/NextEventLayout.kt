package rocks.urzahil.confnav.wear

import android.content.ComponentName
import android.graphics.Color
import androidx.wear.protolayout.ActionBuilders.doubleExtra
import androidx.wear.protolayout.ActionBuilders.launchAction
import androidx.wear.protolayout.ActionBuilders.stringExtra
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_START
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.TEXT_ALIGN_CENTER
import androidx.wear.protolayout.LayoutElementBuilders.TEXT_ALIGN_START
import androidx.wear.protolayout.LayoutElementBuilders.TEXT_OVERFLOW_ELLIPSIZE
import androidx.wear.protolayout.LayoutElementBuilders.VERTICAL_ALIGN_CENTER
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.layout.androidImageResource
import androidx.wear.protolayout.layout.box
import androidx.wear.protolayout.layout.column
import androidx.wear.protolayout.layout.imageResource
import androidx.wear.protolayout.layout.row
import androidx.wear.protolayout.layout.spacer
import androidx.wear.protolayout.material3.ButtonColors
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.card
import androidx.wear.protolayout.material3.icon
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.textEdgeButton
import androidx.wear.protolayout.modifiers.LayoutModifier
import androidx.wear.protolayout.modifiers.background
import androidx.wear.protolayout.modifiers.border
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.protolayout.modifiers.clip
import androidx.wear.protolayout.modifiers.contentDescription
import androidx.wear.protolayout.modifiers.padding
import androidx.wear.protolayout.types.argb
import androidx.wear.protolayout.types.layoutString
import rocks.urzahil.confnav.shared.TimelineSlot
import rocks.urzahil.confnav.shared.WatchSession

/*
 * The tile uses its own layout rather than Material's primaryLayout: that one reserves a header
 * where the system draws the app icon, which would take space from the session card.
 */

/** Card inset from the top of the screen, as a fraction of its width; keeps the corners on screen. */
private const val TOP_INSET = 0.125f

/** Card inset from the sides of the screen, as a fraction of its width. */
private const val SIDE_INSET = 0.1f

/** Card corner radius, as a fraction of the screen width. */
private const val CORNER_RADIUS = 0.12f

/**
 * The tile for one timeline slot, laid out like the phone's "next event" pill: a card in the
 * session's pin colour filling the screen, with Directions as the edge button below it.
 */
fun MaterialScope.nextEventLayout(slot: TimelineSlot): LayoutElement {
    val session = slot.session
    val accent = ConfNavWearTheme.sessionColor(session.colorHex)

    return screen(
        content = { sessionCard(slot, accent) },
        bottom = if (session.hasLocation) {
            {
                textEdgeButton(
                    onClick = directionsClickable(session),
                    modifier = LayoutModifier.contentDescription("Directions to ${session.locationName}"),
                    colors = ButtonColors(
                        containerColor = accent.argb,
                        iconColor = Color.WHITE.argb,
                        labelColor = Color.WHITE.argb,
                        secondaryLabelColor = Color.WHITE.argb
                    )
                ) { text("Directions".layoutString) }
            }
        } else {
            null
        }
    )
}

/** Shown when no session is left, or before the phone has synced an agenda. */
fun MaterialScope.noEventLayout(hasAgenda: Boolean): LayoutElement =
    screen(
        content = {
            column(
                text("ConfNav".layoutString, typography = Typography.TITLE_SMALL, color = colorScheme.primary),
                spacer(height = dp(6f)),
                text(
                    text = (if (hasAgenda) "No more sessions in your agenda"
                    else "Open ConfNav on your phone to sync your agenda").layoutString,
                    typography = Typography.BODY_MEDIUM,
                    color = colorScheme.onSurface,
                    maxLines = 3,
                    alignment = TEXT_ALIGN_CENTER
                ),
                width = expand(),
                horizontalAlignment = HORIZONTAL_ALIGN_CENTER
            )
        },
        bottom = null
    )

/**
 * The screen's frame: [content] inset to stay clear of the round edge, filling the space above
 * [bottom] (the edge button), or the whole screen when there is no bottom element.
 */
private fun MaterialScope.screen(
    content: MaterialScope.() -> LayoutElement,
    bottom: (MaterialScope.() -> LayoutElement)?
): LayoutElement {
    val width = deviceConfiguration.screenWidthDp.toFloat()
    val top = TOP_INSET * width
    val side = SIDE_INSET * width
    return column(
        box(
            content(),
            width = expand(),
            height = expand(),
            modifier = LayoutModifier.padding(
                start = side,
                top = top,
                end = side,
                bottom = if (bottom == null) top else 4f
            ),
            verticalAlignment = VERTICAL_ALIGN_CENTER
        ),
        *listOfNotNull(bottom?.invoke(this)).toTypedArray(),
        width = expand(),
        height = expand(),
        horizontalAlignment = HORIZONTAL_ALIGN_CENTER
    )
}

/** The session card: UP NEXT (or NOW) and a time, then the title, venue and speaker. */
private fun MaterialScope.sessionCard(slot: TimelineSlot, accent: Int): LayoutElement {
    val session = slot.session
    val radius = CORNER_RADIUS * deviceConfiguration.screenWidthDp

    val lines = listOfNotNull(
        // The full "09:00 – 10:00" range does not fit beside the label on small watches, so show
        // the time that matters now: when it starts, or when it ends once it is on.
        row(
            text(
                text = (if (slot.ongoing) "NOW" else "UP NEXT").layoutString,
                typography = Typography.BODY_SMALL,
                color = accent.argb
            ),
            text(
                text = " · ${if (slot.ongoing) "until ${session.endTime}" else session.startTime}".layoutString,
                typography = Typography.BODY_SMALL,
                color = colorScheme.onSurfaceVariant,
                maxLines = 1
            ),
            verticalAlignment = VERTICAL_ALIGN_CENTER
        ),
        spacer(height = dp(3f)),
        text(
            text = (if (session.starred) "★ ${session.title}" else session.title).layoutString,
            typography = Typography.LABEL_SMALL,
            color = colorScheme.onSurface,
            maxLines = 2,
            alignment = TEXT_ALIGN_START,
            overflow = TEXT_OVERFLOW_ELLIPSIZE
        ),
        spacer(height = dp(3f)),
        locationRow(session, accent),
        session.speaker.takeIf { it.isNotBlank() }?.let { speaker ->
            text(
                text = speaker.layoutString,
                typography = Typography.BODY_SMALL,
                color = colorScheme.onSurfaceVariant,
                maxLines = 1,
                alignment = TEXT_ALIGN_START,
                overflow = TEXT_OVERFLOW_ELLIPSIZE
            )
        }
    )

    return card(
        // Tapping the card just refreshes the tile; directions have their own button.
        onClick = clickable(),
        modifier = LayoutModifier
            .background(colorScheme.surfaceContainer)
            .clip(radius)
            .border(1f, ConfNavWearTheme.withAlpha(accent, 0.4f).argb),
        width = expand(),
        height = expand(),
        contentPadding = padding(horizontal = radius * 0.6f, vertical = 8f)
    ) {
        column(
            *lines.toTypedArray(),
            width = expand(),
            horizontalAlignment = HORIZONTAL_ALIGN_START
        )
    }
}

private fun MaterialScope.locationRow(session: WatchSession, accent: Int): LayoutElement =
    row(
        icon(
            resource = imageResource(androidImage = androidImageResource(R.drawable.ic_location)),
            width = dp(12f),
            height = dp(12f),
            tintColor = accent.argb
        ),
        spacer(width = dp(3f)),
        text(
            text = session.locationName.layoutString,
            typography = Typography.BODY_SMALL,
            color = accent.argb,
            maxLines = 1,
            alignment = TEXT_ALIGN_START,
            overflow = TEXT_OVERFLOW_ELLIPSIZE
        ),
        verticalAlignment = VERTICAL_ALIGN_CENTER
    )

private fun MaterialScope.directionsClickable(session: WatchSession): Clickable =
    clickable(
        action = launchAction(
            ComponentName(context, DirectionsActivity::class.java),
            mapOf(
                DirectionsActivity.EXTRA_LATITUDE to doubleExtra(session.latitude),
                DirectionsActivity.EXTRA_LONGITUDE to doubleExtra(session.longitude),
                DirectionsActivity.EXTRA_NAME to stringExtra(session.locationName)
            )
        ),
        id = "directions"
    )
