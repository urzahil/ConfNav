package rocks.urzahil.confnav.wear

import android.content.ComponentName
import android.graphics.Color
import androidx.wear.protolayout.ActionBuilders.doubleExtra
import androidx.wear.protolayout.ActionBuilders.launchAction
import androidx.wear.protolayout.ActionBuilders.stringExtra
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.TEXT_ALIGN_START
import androidx.wear.protolayout.LayoutElementBuilders.TEXT_OVERFLOW_ELLIPSIZE
import androidx.wear.protolayout.LayoutElementBuilders.VERTICAL_ALIGN_CENTER
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.layout.androidImageResource
import androidx.wear.protolayout.layout.column
import androidx.wear.protolayout.layout.imageResource
import androidx.wear.protolayout.layout.row
import androidx.wear.protolayout.layout.spacer
import androidx.wear.protolayout.material3.ButtonColors
import androidx.wear.protolayout.material3.CardColors
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.icon
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.textEdgeButton
import androidx.wear.protolayout.material3.titleCard
import androidx.wear.protolayout.modifiers.LayoutModifier
import androidx.wear.protolayout.modifiers.border
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.protolayout.modifiers.contentDescription
import androidx.wear.protolayout.types.argb
import androidx.wear.protolayout.types.layoutString
import rocks.urzahil.confnav.shared.TimelineSlot
import rocks.urzahil.confnav.shared.WatchSession

/**
 * The tile for one timeline slot, laid out like the phone's "next event" pill: an UP NEXT (or
 * NOW) label and the session card in the session's pin colour, with Directions as the edge button.
 */
fun MaterialScope.nextEventLayout(slot: TimelineSlot): LayoutElement {
    val session = slot.session
    val accent = ConfNavWearTheme.sessionColor(session.colorHex)

    return primaryLayout(
        titleSlot = {
            text(
                text = (if (slot.ongoing) "NOW" else "UP NEXT").layoutString,
                typography = Typography.LABEL_MEDIUM,
                color = accent.argb
            )
        },
        mainSlot = { sessionCard(session, accent) },
        bottomSlot = if (session.hasLocation) {
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
    primaryLayout(
        titleSlot = { text("ConfNav".layoutString) },
        mainSlot = {
            text(
                text = (if (hasAgenda) "No more sessions in your agenda"
                else "Open ConfNav on your phone to sync your agenda").layoutString,
                typography = Typography.BODY_MEDIUM,
                maxLines = 3
            )
        }
    )

private fun MaterialScope.sessionCard(session: WatchSession, accent: Int): LayoutElement =
    titleCard(
        // Tapping the card just refreshes the tile; directions have their own button.
        onClick = clickable(),
        modifier = LayoutModifier.border(1f, ConfNavWearTheme.withAlpha(accent, 0.4f).argb),
        height = expand(),
        colors = CardColors(
            backgroundColor = colorScheme.surfaceContainer,
            titleColor = colorScheme.onSurface,
            contentColor = colorScheme.onSurfaceVariant,
            timeColor = colorScheme.onSurfaceVariant
        ),
        time = { text("${session.startTime} – ${session.endTime}".layoutString) },
        title = {
            text(
                text = (if (session.starred) "★ ${session.title}" else session.title).layoutString,
                maxLines = 2,
                alignment = TEXT_ALIGN_START,
                overflow = TEXT_OVERFLOW_ELLIPSIZE
            )
        },
        content = {
            column(
                locationRow(session, accent),
                *listOfNotNull(
                    session.speaker.takeIf { it.isNotBlank() }?.let { speaker ->
                        text(
                            text = speaker.layoutString,
                            typography = Typography.BODY_SMALL,
                            maxLines = 1,
                            alignment = TEXT_ALIGN_START,
                            overflow = TEXT_OVERFLOW_ELLIPSIZE
                        )
                    }
                ).toTypedArray(),
                width = expand()
            )
        }
    )

private fun MaterialScope.locationRow(session: WatchSession, accent: Int): LayoutElement =
    row(
        icon(
            resource = imageResource(androidImage = androidImageResource(R.drawable.ic_location)),
            width = dp(14f),
            height = dp(14f),
            tintColor = accent.argb
        ),
        spacer(width = dp(3f)),
        text(
            text = session.locationName.layoutString,
            typography = Typography.LABEL_SMALL,
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
