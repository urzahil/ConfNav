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
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import rocks.urzahil.confnav.ui.ConferenceViewModel
import rocks.urzahil.confnav.ui.components.NextEventPill
import rocks.urzahil.confnav.ui.components.PinMarkerCanvas
import rocks.urzahil.confnav.ui.components.openDirections
import rocks.urzahil.confnav.ui.icons.filled.MyLocation
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
fun ConferenceMapScreen(
    viewModel: ConferenceViewModel,
    onNavigateToConfigure: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    var mapHeaderHeightPx by remember { mutableStateOf(0) }
    val mapHeaderHeight = with(density) { mapHeaderHeightPx.toDp() }

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

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val isDarkTheme = isSystemInDarkTheme()
    val mapStyleOptions = remember(isDarkTheme) {
        try {
            if (isDarkTheme) {
                MapStyleOptions(DARK_MAP_STYLE_JSON)
            } else {
                MapStyleOptions(LIGHT_MAP_STYLE_CLEAN_JSON)
            }
        } catch (_: Exception) {
            null
        }
    }

    Box(modifier = modifier
        .fillMaxSize()
        .testTag("conference_map_screen")) {
        // 1. Map layer
        GoogleMap(
            modifier = Modifier
                .fillMaxSize()
                .testTag("google_map_view"),
            cameraPositionState = cameraPositionState,
            properties = remember(mapStyleOptions, hasLocationPermission) {
                MapProperties(
                    isMyLocationEnabled = hasLocationPermission,
                    isBuildingEnabled = true,
                    isIndoorEnabled = true,
                    mapType = MapType.NORMAL,
                    mapStyleOptions = mapStyleOptions
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
                // Keyed by venue so each marker keeps its own remembered state and icon.
                key(locationGroup.key) {
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
                    val markerState = remember(locationGroup.latitude, locationGroup.longitude) {
                        MarkerState(position = LatLng(locationGroup.latitude, locationGroup.longitude))
                    }

                    Marker(
                        state = markerState,
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
        }

        // 2. Top Bar with Filter Chips & Next Event Pill
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                .onSizeChanged { mapHeaderHeightPx = it.height }
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
                .padding(top = mapHeaderHeight + 16.dp, end = 16.dp)
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

