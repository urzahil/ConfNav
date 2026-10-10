package com.example

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.Map
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
import com.example.ui.ConferenceViewModel
import com.example.ui.manage.ConfigureSessionScreen
import com.example.ui.map.ConferenceMapScreen
import com.example.ui.schedule.ScheduleScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                ConferenceApp()
            }
        }
    }
}

enum class NavigationTab(val title: String) {
    AGENDA("Agenda"),
    MAP("Map"),
    CONFIGURE("Configure");

    companion object {
        val SCHEDULE = AGENDA
    }
}

@Composable
fun ConferenceApp(
    viewModel: ConferenceViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf(NavigationTab.AGENDA) }
    var editingSessionId by remember { mutableStateOf<Long?>(null) }
    var editOriginTab by remember { mutableStateOf(NavigationTab.CONFIGURE) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Back handling: pop back to Agenda screen if on sub-screens
    BackHandler(enabled = selectedTab != NavigationTab.AGENDA || uiState.selectedLocationPin != null) {
        if (uiState.selectedLocationPin != null) {
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
                    onClick = { selectedTab = NavigationTab.AGENDA },
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

                // Tab 2: Map
                NavigationBarItem(
                    selected = selectedTab == NavigationTab.MAP,
                    onClick = { selectedTab = NavigationTab.MAP },
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

                // Tab 3: Configure / Manage
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
                        onNavigateToMap = { session ->
                            if (session != null) {
                                val targetGroup = uiState.locationGroups.firstOrNull { group ->
                                    group.sessions.any { it.id == session.id }
                                }
                                if (targetGroup != null) {
                                    viewModel.selectLocationPin(targetGroup)
                                }
                            }
                            selectedTab = NavigationTab.MAP
                        },
                        onNavigateToConfigure = { sessionId ->
                            editOriginTab = NavigationTab.AGENDA
                            editingSessionId = sessionId
                            selectedTab = NavigationTab.CONFIGURE
                        }
                    )
                }

                NavigationTab.MAP -> {
                    ConferenceMapScreen(
                        viewModel = viewModel,
                        onNavigateToConfigure = { sessionId ->
                            editOriginTab = NavigationTab.MAP
                            editingSessionId = sessionId
                            selectedTab = NavigationTab.CONFIGURE
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
