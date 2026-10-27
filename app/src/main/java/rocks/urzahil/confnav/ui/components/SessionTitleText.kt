package rocks.urzahil.confnav.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.em
import rocks.urzahil.confnav.data.model.ConferenceSession

/** Colour of the star shown next to starred sessions. */
val StarGold = Color(0xFFFFC107)

private const val STAR_ID = "star"

/**
 * A session's title, led by a gold star when the session is starred. The star is inline with the
 * text, so it stays at the left of the first line however the title wraps or is cut off.
 */
@Composable
fun SessionTitleText(
    session: ConferenceSession,
    modifier: Modifier = Modifier,
    text: String = session.title,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    if (!session.starred) {
        Text(
            text = text,
            modifier = modifier,
            style = style,
            color = color,
            fontWeight = fontWeight,
            maxLines = maxLines,
            overflow = overflow
        )
        return
    }

    val annotated = buildAnnotatedString {
        appendInlineContent(STAR_ID, "★")
        append(" ")
        append(text)
    }
    val inlineContent = mapOf(
        STAR_ID to InlineTextContent(
            Placeholder(width = 1.1.em, height = 1.1.em, placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter)
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = "Starred",
                tint = StarGold,
                modifier = Modifier.fillMaxSize()
            )
        }
    )
    Text(
        text = annotated,
        inlineContent = inlineContent,
        modifier = modifier,
        style = style,
        color = color,
        fontWeight = fontWeight,
        maxLines = maxLines,
        overflow = overflow
    )
}
