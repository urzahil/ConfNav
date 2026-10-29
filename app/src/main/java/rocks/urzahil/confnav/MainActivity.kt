package rocks.urzahil.confnav

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.CameraPosition
import rocks.urzahil.confnav.data.model.ConferenceSession
import rocks.urzahil.confnav.ui.ConferenceViewModel
import rocks.urzahil.confnav.ui.icons.filled.CalendarMonth
import rocks.urzahil.confnav.ui.icons.filled.EditCalendar
import rocks.urzahil.confnav.ui.icons.filled.Map
import rocks.urzahil.confnav.ui.icons.outlined.CalendarMonth
import rocks.urzahil.confnav.ui.icons.outlined.EditCalendar
import rocks.urzahil.confnav.ui.icons.outlined.Map
import rocks.urzahil.confnav.ui.manage.ConfigureSessionScreen
import rocks.urzahil.confnav.ui.map.ConferenceMapScreen
import rocks.urzahil.confnav.ui.schedule.CalendarDayScreen
import rocks.urzahil.confnav.ui.schedule.ScheduleScreen
import rocks.urzahil.confnav.ui.theme.ConfNavTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ConfNavTheme {
                ConferenceApp()
            }
        }
    }
}

enum class NavigationTab(val title: String) {
    AGENDA("Agenda"),
    CALENDAR("Calendar"),
    MAP("Map"),
    CONFIGURE("Configure");
}

@Composable
fun ConferenceApp(
    viewModel: ConferenceViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf(NavigationTab.AGENDA) }
    var editingSessionId by remember { mutableStateOf<Long?>(null) }
    var editOriginTab by remember { mutableStateOf(NavigationTab.CONFIGURE) }
    // The Agenda or Calendar tab that opened the Map for a session, if any.
    var mapOriginTab by remember { mutableStateOf<NavigationTab?>(null) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Kept out here so the Agenda, Calendar and Map keep their position while another tab is
    // shown; the lists only jump to today, and the Map only fits all venues, when first opened.
    val agendaListState = rememberLazyListState()
    var agendaScrollToNow by remember { mutableStateOf(true) }
    val calendarListState = rememberLazyListState()
    var calendarScrollToNow by remember { mutableStateOf(true) }
    var mapCameraPosition by remember { mutableStateOf<CameraPosition?>(null) }

    // Opens the Map tab, with the session's venue pin selected when a session is given.
    val navigateToMap: (ConferenceSession?) -> Unit = { session ->
        if (session != null) {
            val targetGroup = uiState.locationGroups.firstOrNull { group ->
                group.sessions.any { it.id == session.id }
            }
            if (targetGroup != null) {
                viewModel.selectLocationPin(targetGroup)
            }
        }
        if (selectedTab == NavigationTab.AGENDA || selectedTab == NavigationTab.CALENDAR) {
            mapOriginTab = selectedTab
        }
        selectedTab = NavigationTab.MAP
    }

    // Leaves a Map opened from a session the way it came, keeping that tab's scroll position.
    val returnFromMap: (NavigationTab) -> Unit = { origin ->
        viewModel.selectLocationPin(null)
        mapOriginTab = null
        selectedTab = origin
    }

    // Back handling: pop back to Agenda screen if on sub-screens
    BackHandler(enabled = selectedTab != NavigationTab.AGENDA || uiState.selectedLocationPin != null) {
        val origin = mapOriginTab
        if (selectedTab == NavigationTab.MAP && origin != null) {
            returnFromMap(origin)
        } else if (uiState.selectedLocationPin != null) {
            viewModel.selectLocationPin(null)
        } else if (selectedTab != NavigationTab.AGENDA) {
            selectedTab = NavigationTab.AGENDA
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_navigation_bar"),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                // Tab 1: Agenda / Calendar
                NavigationBarItem(
                    selected = selectedTab == NavigationTab.AGENDA,
                    onClick = {
                        selectedTab = NavigationTab.AGENDA
                    },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == NavigationTab.AGENDA) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth,
                            contentDescription = "Agenda"
                        )
                    },
                    label = {
                        Text(
                            text = NavigationTab.AGENDA.title,
                            fontWeight = if (selectedTab == NavigationTab.AGENDA) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("nav_tab_schedule")
                )

                // Tab 2: Calendar day grid
                NavigationBarItem(
                    selected = selectedTab == NavigationTab.CALENDAR,
                    onClick = {
                        selectedTab = NavigationTab.CALENDAR
                    },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == NavigationTab.CALENDAR) Icons.Filled.DateRange else Icons.Outlined.DateRange,
                            contentDescription = "Calendar"
                        )
                    },
                    label = {
                        Text(
                            text = NavigationTab.CALENDAR.title,
                            fontWeight = if (selectedTab == NavigationTab.CALENDAR) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("nav_tab_calendar")
                )

                // Tab 3: Map
                NavigationBarItem(
                    selected = selectedTab == NavigationTab.MAP,
                    onClick = {
                        mapOriginTab = null
                        selectedTab = NavigationTab.MAP
                    },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == NavigationTab.MAP) Icons.Filled.Map else Icons.Outlined.Map,
                            contentDescription = "Map"
                        )
                    },
                    label = {
                        Text(
                            text = NavigationTab.MAP.title,
                            fontWeight = if (selectedTab == NavigationTab.MAP) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("nav_tab_map")
                )

                // Tab 4: Configure / Manage
                NavigationBarItem(
                    selected = selectedTab == NavigationTab.CONFIGURE,
                    onClick = {
                        editOriginTab = NavigationTab.CONFIGURE
                        editingSessionId = null
                        selectedTab = NavigationTab.CONFIGURE
                    },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == NavigationTab.CONFIGURE) Icons.Filled.EditCalendar else Icons.Outlined.EditCalendar,
                            contentDescription = "Configure"
                        )
                    },
                    label = {
                        Text(
                            text = NavigationTab.CONFIGURE.title,
                            fontWeight = if (selectedTab == NavigationTab.CONFIGURE) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("nav_tab_configure")
                )
            }
        }
    ) { innerPadding ->
        Crossfade(
            targetState = selectedTab,
            label = "screen_crossfade",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { tab ->
            when (tab) {
                NavigationTab.AGENDA -> {
                    ScheduleScreen(
                        viewModel = viewModel,
                        listState = agendaListState,
                        scrollToNow = agendaScrollToNow,
                        onScrollToNowChange = { agendaScrollToNow = it },
                        onNavigateToMap = navigateToMap,
                        onNavigateToConfigure = { sessionId ->
                            editOriginTab = NavigationTab.AGENDA
                            editingSessionId = sessionId
                            selectedTab = NavigationTab.CONFIGURE
                        }
                    )
                }

                NavigationTab.CALENDAR -> {
                    CalendarDayScreen(
                        viewModel = viewModel,
                        listState = calendarListState,
                        scrollToNow = calendarScrollToNow,
                        onScrollToNowChange = { calendarScrollToNow = it },
                        onNavigateToMap = navigateToMap,
                        onNavigateToConfigure = { sessionId ->
                            editOriginTab = NavigationTab.CALENDAR
                            editingSessionId = sessionId
                            selectedTab = NavigationTab.CONFIGURE
                        }
                    )
                }

                NavigationTab.MAP -> {
                    ConferenceMapScreen(
                        viewModel = viewModel,
                        savedCameraPosition = mapCameraPosition,
                        onCameraPositionSaved = { mapCameraPosition = it },
                        onNavigateToConfigure = { sessionId ->
                            editOriginTab = NavigationTab.MAP
                            editingSessionId = sessionId
                            selectedTab = NavigationTab.CONFIGURE
                        },
                        onCloseLocationCard = {
                            val origin = mapOriginTab
                            if (origin != null) returnFromMap(origin) else viewModel.selectLocationPin(null)
                        }
                    )
                }

                NavigationTab.CONFIGURE -> {
                    ConfigureSessionScreen(
                        viewModel = viewModel,
                        editingSessionId = editingSessionId,
                        onSessionSaved = {
                            editingSessionId = null
                            selectedTab = editOriginTab
                        },
                        onEditCancelled = {
                            editingSessionId = null
                            selectedTab = editOriginTab
                        }
                    )
                }
            }
        }
    }
}
