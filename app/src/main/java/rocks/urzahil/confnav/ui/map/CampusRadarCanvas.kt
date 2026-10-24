package rocks.urzahil.confnav.ui.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import rocks.urzahil.confnav.data.model.LocationGroup
import rocks.urzahil.confnav.ui.ConferenceViewModel
import rocks.urzahil.confnav.ui.components.NextEventPill
import rocks.urzahil.confnav.ui.components.PinColorHelper
import rocks.urzahil.confnav.ui.components.PinMarkerCanvas
import rocks.urzahil.confnav.ui.components.openDirections
import rocks.urzahil.confnav.ui.schedule.DayFilterChipsRow
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

@Composable
fun CampusRadarCanvas(
    locationGroups: List<LocationGroup>,
    selectedLocation: LocationGroup?,
    onSelectLocation: (LocationGroup) -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val textMeasurer = rememberTextMeasurer()

    val canvasBg = MaterialTheme.colorScheme.surfaceContainerLowest
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    val pathColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val bubbleColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f)
    val titleTextColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .background(canvasBg)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.6f, 3.5f)
                    offsetX += pan.x
                    offsetY += pan.y
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(locationGroups, selectedLocation, scale, offsetX, offsetY) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: continue
                            if (change.pressed) {
                                // Test if touch hit any venue pin
                                val touchX = change.position.x
                                val touchY = change.position.y

                                val w = size.width.toFloat()
                                val h = size.height.toFloat()

                                val lats = locationGroups.map { it.latitude }
                                val lngs = locationGroups.map { it.longitude }
                                val minLat = lats.minOrNull() ?: 37.7830
                                val maxLat = lats.maxOrNull() ?: 37.7870
                                val minLng = lngs.minOrNull() ?: -122.4050
                                val maxLng = lngs.maxOrNull() ?: -122.4000

                                val latSpan = (maxLat - minLat).coerceAtLeast(0.0020)
                                val lngSpan = (maxLng - minLng).coerceAtLeast(0.0020)

                                for (loc in locationGroups) {
                                    val normX = ((loc.longitude - minLng) / lngSpan).toFloat()
                                    val normY = (1f - ((loc.latitude - minLat) / latSpan).toFloat())

                                    val pinX = (w * 0.15f + normX * w * 0.70f) * scale + offsetX
                                    val pinY = (h * 0.20f + normY * h * 0.60f) * scale + offsetY

                                    val distSq =
                                        (touchX - pinX) * (touchX - pinX) + (touchY - pinY) * (touchY - pinY)
                                    if (distSq < (40 * scale) * (40 * scale)) {
                                        onSelectLocation(loc)
                                        change.consume()
                                        break
                                    }
                                }
                            }
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height

            // 1. Grid lines
            val gridSize = 40f * scale
            var curX = (offsetX % gridSize)
            while (curX < w) {
                drawLine(gridColor, Offset(curX, 0f), Offset(curX, h), strokeWidth = 1f)
                curX += gridSize
            }
            var curY = (offsetY % gridSize)
            while (curY < h) {
                drawLine(gridColor, Offset(0f, curY), Offset(w, curY), strokeWidth = 1f)
                curY += gridSize
            }

            if (locationGroups.isEmpty()) return@Canvas

            // Compute coordinates mapping
            val lats = locationGroups.map { it.latitude }
            val lngs = locationGroups.map { it.longitude }
            val minLat = lats.minOrNull() ?: 37.7830
            val maxLat = lats.maxOrNull() ?: 37.7870
            val minLng = lngs.minOrNull() ?: -122.4050
            val maxLng = lngs.maxOrNull() ?: -122.4000

            val latSpan = (maxLat - minLat).coerceAtLeast(0.0020)
            val lngSpan = (maxLng - minLng).coerceAtLeast(0.0020)

            val pinOffsets = locationGroups.map { loc ->
                val normX = ((loc.longitude - minLng) / lngSpan).toFloat()
                val normY = (1f - ((loc.latitude - minLat) / latSpan).toFloat())
                Offset(
                    x = (w * 0.15f + normX * w * 0.70f) * scale + offsetX,
                    y = (h * 0.20f + normY * h * 0.60f) * scale + offsetY
                )
            }

            // 2. Draw walking pathway connections between nearby venues
            for (i in pinOffsets.indices) {
                for (j in i + 1 until pinOffsets.size) {
                    val p1 = pinOffsets[i]
                    val p2 = pinOffsets[j]
                    drawLine(
                        color = pathColor,
                        start = p1,
                        end = p2,
                        strokeWidth = 2f * scale,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            floatArrayOf(10f * scale, 10f * scale),
                            0f
                        )
                    )
                }
            }

            // 3. Draw venue location pins with distinct colors and badges
            locationGroups.forEachIndexed { index, loc ->
                val center = pinOffsets[index]
                val locColor = PinColorHelper.parseColor(loc.colorHex)
                val isSelected = selectedLocation?.key == loc.key
                val pinRadius = if (isSelected) 24f * scale else 18f * scale

                // Ripple ring if selected
                if (isSelected) {
                    drawCircle(
                        color = locColor.copy(alpha = 0.25f),
                        radius = pinRadius * 1.8f,
                        center = center
                    )
                    drawCircle(
                        color = locColor.copy(alpha = 0.5f),
                        radius = pinRadius * 1.4f,
                        center = center,
                        style = Stroke(width = 2f * scale)
                    )
                }

                // Shadow
                drawOval(
                    color = Color(0x33000000),
                    topLeft = Offset(center.x - pinRadius * 0.8f, center.y + pinRadius * 0.6f),
                    size = Size(pinRadius * 1.6f, pinRadius * 0.6f)
                )

                // Pin body
                drawCircle(
                    color = locColor,
                    radius = pinRadius,
                    center = center
                )
                drawCircle(
                    color = Color.White,
                    radius = pinRadius,
                    center = center,
                    style = Stroke(width = 2.5f * scale)
                )

                // Inner white badge
                drawCircle(
                    color = Color.White,
                    radius = pinRadius * 0.6f,
                    center = center
                )

                // Text: event count
                val label = loc.sessionCount.toString()
                val textLayout = textMeasurer.measure(
                    text = label,
                    style = TextStyle(
                        color = locColor,
                        fontSize = (12 * scale).sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                drawText(
                    textLayoutResult = textLayout,
                    topLeft = Offset(
                        center.x - textLayout.size.width / 2f,
                        center.y - textLayout.size.height / 2f
                    )
                )

                // Venue Title text below pin
                val nameLayout = textMeasurer.measure(
                    text = loc.locationName,
                    style = TextStyle(
                        color = titleTextColor,
                        fontSize = (11 * scale).sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                // Background bubble for title
                val labelWidth = nameLayout.size.width + 12f * scale
                val labelHeight = nameLayout.size.height + 6f * scale
                val labelX = center.x - labelWidth / 2f
                val labelY = center.y + pinRadius + 6f * scale

                drawRoundRect(
                    color = bubbleColor,
                    topLeft = Offset(labelX, labelY),
                    size = Size(labelWidth, labelHeight),
                    cornerRadius = CornerRadius(6f * scale, 6f * scale)
                )
                drawText(
                    textLayoutResult = nameLayout,
                    topLeft = Offset(labelX + 6f * scale, labelY + 3f * scale)
                )
            }
        }

        // Radar mode banner
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Text(
                text = "Campus Radar View • Pinch to zoom, tap pins for sessions",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

