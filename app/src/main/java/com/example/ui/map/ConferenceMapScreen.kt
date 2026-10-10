package com.example.ui.map

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.LocationGroup
import com.example.ui.ConferenceViewModel
import com.example.ui.components.NextEventPill
import com.example.ui.components.PinColorHelper
import com.example.ui.components.PinMarkerCanvas
import com.example.ui.components.openDirections
import com.example.ui.schedule.DayFilterChipsRow
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConferenceMapScreen(
    viewModel: ConferenceViewModel,
    onNavigateToConfigure: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Mode: Google Map or Campus Radar Map
    var isRadarMode by remember { mutableStateOf(false) }

    // Map Camera
    val defaultCenter = remember(uiState.locationGroups) {
        if (uiState.locationGroups.isNotEmpty()) {
            val avgLat = uiState.locationGroups.map { it.latitude }.average()
            val avgLng = uiState.locationGroups.map { it.longitude }.average()
            LatLng(avgLat, avgLng)
        } else {
            LatLng(37.7849, -122.4020)
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultCenter, 16.5f)
    }

    // Camera animation when selected pin changes
    LaunchedEffect(uiState.selectedLocationPin) {
        val pin = uiState.selectedLocationPin
        if (pin != null) {
            try {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(pin.latitude, pin.longitude),
                        17.5f
                    ),
                    700
                )
            } catch (_: Exception) {
            }
        }
    }

    // Function to re-center on all pins
    val fitAllPins: () -> Unit = {
        if (uiState.locationGroups.isNotEmpty()) {
            coroutineScope.launch {
                try {
                    val builder = LatLngBounds.builder()
                    uiState.locationGroups.forEach {
                        builder.include(LatLng(it.latitude, it.longitude))
                    }
                    val bounds = builder.build()
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngBounds(bounds, 120),
                        800
                    )
                } catch (_: Exception) {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(
                            defaultCenter,
                            16.2f
                        ), 800
                    )
                }
            }
        }
    }

    Box(modifier = modifier
        .fillMaxSize()
        .testTag("conference_map_screen")) {
        // 1. Map Canvas Layer: Google Maps or Campus Radar Canvas
        if (!isRadarMode) {
            GoogleMap(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("google_map_view"),
                cameraPositionState = cameraPositionState,
                properties = remember {
                    MapProperties(
                        isBuildingEnabled = true,
                        isIndoorEnabled = true,
                        mapType = MapType.NORMAL
                    )
                },
                uiSettings = remember {
                    MapUiSettings(
                        zoomControlsEnabled = false,
                        compassEnabled = true,
                        mapToolbarEnabled = false
                    )
                },
                onMapClick = {
                    viewModel.selectLocationPin(null)
                }
            ) {
                // One pin per unique location, matching the color of sessions held there!
                uiState.locationGroups.forEach { locationGroup ->
                    val isSelected = uiState.selectedLocationPin?.key == locationGroup.key
                    val markerIcon =
                        remember(locationGroup.colorHex, locationGroup.sessionCount, isSelected) {
                            PinMarkerCanvas.createPinBitmap(
                                colorHex = locationGroup.colorHex,
                                count = locationGroup.sessionCount,
                                isSelected = isSelected,
                                scale = if (isSelected) 2.5f else 2.0f
                            )
                        }

                    Marker(
                        state = MarkerState(
                            position = LatLng(
                                locationGroup.latitude,
                                locationGroup.longitude
                            )
                        ),
                        title = locationGroup.locationName,
                        snippet = "${locationGroup.sessionCount} sessions",
                        icon = markerIcon,
                        onClick = {
                            viewModel.selectLocationPin(locationGroup)
                            true
                        }
                    )
                }
            }
        } else {
            // Interactive 2D Campus Radar Fallback
            CampusRadarCanvas(
                locationGroups = uiState.locationGroups,
                selectedLocation = uiState.selectedLocationPin,
                onSelectLocation = { viewModel.selectLocationPin(it) },
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. Top Bar with Filter Chips & Next Event Pill
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                .padding(bottom = 6.dp)
        ) {
            // Day Filter Chips
            DayFilterChipsRow(
                filters = uiState.availableFilters,
                selectedFilter = uiState.selectedFilter,
                onFilterSelected = { viewModel.setDayFilter(it) }
            )

            // Next Event Pill with "Directions"
            if (uiState.nextSession != null) {
                NextEventPill(
                    session = uiState.nextSession,
                    onNavigateToMap = { session ->
                        val targetGroup = uiState.locationGroups.firstOrNull { group ->
                            group.sessions.any { it.id == session.id }
                        }
                        if (targetGroup != null) {
                            viewModel.selectLocationPin(targetGroup)
                        }
                    }
                )
            }
        }

        // 3. Floating Action Controls: Center on venues button at top right of the map
        FloatingActionButton(
            onClick = fitAllPins,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(
                    top = if (uiState.nextSession != null) 114.dp else 56.dp,
                    end = 14.dp
                )
                .size(44.dp)
                .testTag("btn_fit_pins")
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Center on Venues",
                modifier = Modifier.size(20.dp)
            )
        }

        // 4. Pin Info Card / Bottom Sheet showing list of events at this location
        val selectedPin = uiState.selectedLocationPin
        AnimatedVisibility(
            visible = selectedPin != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            if (selectedPin != null) {
                LocationDetailsCard(
                    locationGroup = selectedPin,
                    onClose = { viewModel.selectLocationPin(null) },
                    onOpenDirections = {
                        val first = selectedPin.sessions.firstOrNull()
                        if (first != null) {
                            openDirections(context, first)
                        }
                    },
                    onEditSession = { sessionId ->
                        onNavigateToConfigure(sessionId)
                    }
                )
            }
        }
    }
}

@Composable
fun LocationDetailsCard(
    locationGroup: LocationGroup,
    onClose: () -> Unit,
    onOpenDirections: () -> Unit,
    onEditSession: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val locationColor = PinColorHelper.parseColor(locationGroup.colorHex)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 380.dp)
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .border(
                1.5.dp,
                locationColor.copy(alpha = 0.4f),
                RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
            .testTag("location_details_sheet"),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            // Drag handle / colored bar matching pin color
            Box(
                modifier = Modifier
                    .size(width = 44.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(locationColor)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Venue Name & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(locationColor)
                    )
                    Column {
                        Text(
                            text = locationGroup.locationName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = locationGroup.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Session Count Banner & Directions Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = locationColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = "${locationGroup.sessionCount} Events Scheduled Here",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = locationColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                ElevatedButton(
                    onClick = onOpenDirections,
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = locationColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Directions,
                        contentDescription = "Directions",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Directions",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // List of events held at this location
            Text(
                text = "Events at this venue:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(locationGroup.sessions, key = { it.id }) { session ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditSession(session.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = session.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "${ConferenceViewModel.formatEuropeanDate(session.date)} • ${session.startTime} - ${session.endTime}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = locationColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "• ${session.speaker}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Interactive 2D Campus Radar Canvas
 * Renders conference campus layout with walking paths, venue pins, and pinch/pan support.
 */
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
