package rocks.urzahil.confnav.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import rocks.urzahil.confnav.data.model.ConferenceSession
import rocks.urzahil.confnav.ui.components.PinColorHelper
import rocks.urzahil.confnav.ui.components.SessionTitleText
import rocks.urzahil.confnav.ui.components.StarGold
import rocks.urzahil.confnav.ui.icons.filled.StarBorder
import rocks.urzahil.confnav.ui.icons.filled.Directions
import rocks.urzahil.confnav.ui.icons.filled.Map

@Composable
fun SessionCalendarCard(
    session: ConferenceSession,
    onOpenDirections: () -> Unit,
    onViewOnMap: () -> Unit,
    onEdit: () -> Unit,
    onToggleStar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locationColor = PinColorHelper.parseColor(session.colorHex)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("session_card_${session.id}"),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Timeline Column (Time + Node + Line)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(48.dp)
        ) {
            Text(
                text = session.startTime,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = session.endTime,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Node matching the pin color
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(locationColor)
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )

            // Vertical connecting line
            Box(
                modifier = Modifier
                    .width(1.5.dp)
                    .weight(1f, fill = false)
                    .height(36.dp)
                    .background(locationColor.copy(alpha = 0.3f))
            )
        }

        // Session Card matching pin color
        Card(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, locationColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Colored Header Strip matching the Pin Color
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(locationColor)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    // Track Tag & Room Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Location Pill matching the PIN color (compact)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(locationColor.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .clickable(enabled = session.hasLocation) { onViewOnMap() }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(locationColor)
                            )
                            Text(
                                text = session.locationName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = locationColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    // Title
                    SessionTitleText(
                        session = session,
                        style = MaterialTheme.typography.titleSmall.copy(lineHeight = 16.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Speaker
                    if (session.speaker.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (session.speakerRole.isNotBlank()) "${session.speaker} (${session.speakerRole})" else session.speaker,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Description if present
                    if (session.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = session.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Bottom Actions: Directions, View on Map, Edit (compact pills)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Directions Button
                            if (session.hasLocation) OutlinedButton(
                                onClick = onOpenDirections,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .height(28.dp)
                                    .testTag("session_directions_${session.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Directions,
                                    contentDescription = null,
                                    tint = locationColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Directions",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = locationColor
                                )
                            }

                            // View on Map Button
                            if (session.hasLocation) OutlinedButton(
                                onClick = onViewOnMap,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Map",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onToggleStar,
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("star_session_${session.id}")
                            ) {
                                Icon(
                                    imageVector = if (session.starred) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = if (session.starred) "Unstar Session" else "Star Session",
                                    tint = if (session.starred) StarGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Edit Button
                            IconButton(
                                onClick = onEdit,
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("edit_session_${session.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Session",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

