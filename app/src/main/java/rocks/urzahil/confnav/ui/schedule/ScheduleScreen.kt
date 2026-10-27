package rocks.urzahil.confnav.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import rocks.urzahil.confnav.data.model.ConferenceSession
import rocks.urzahil.confnav.ui.ConferenceViewModel
import rocks.urzahil.confnav.ui.DayFilter
import rocks.urzahil.confnav.ui.components.NextEventPill
import rocks.urzahil.confnav.ui.components.openDirections
import rocks.urzahil.confnav.ui.icons.filled.Map
import java.time.LocalDate

@Composable
fun ScheduleScreen(
    viewModel: ConferenceViewModel,
    onNavigateToMap: (ConferenceSession?) -> Unit,
    onNavigateToConfigure: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    scrollToNow: Boolean = true,
    onScrollToNowChange: (Boolean) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Scroll back to today when the app returns to the foreground. The observer is also sent
    // the current ON_RESUME as soon as it is added; that one is skipped so coming back from
    // editing a session keeps the list where it was.
    DisposableEffect(lifecycleOwner) {
        var initialResumeSeen = false
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (initialResumeSeen) onScrollToNowChange(true)
                initialResumeSeen = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val groupedByDate = remember(uiState.filteredSessions) { uiState.filteredSessions.groupBy { it.date } }

    // Auto-scroll to current day (or top of list if current day is out of range) when asked to:
    // on opening from the navigation bar, on returning to the foreground, or on picking a filter.
    LaunchedEffect(scrollToNow, uiState.selectedFilter, groupedByDate.isNotEmpty()) {
        if (!scrollToNow || groupedByDate.isEmpty()) return@LaunchedEffect
        if (uiState.selectedFilter is DayFilter.All) {
            listState.scrollToItem(calculateTargetIndex(groupedByDate))
        }
        // Only after scrolling: this changes the effect's key, which would cancel it.
        onScrollToNowChange(false)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("schedule_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar with Filter Chips & Next Event Pill (matching Map exactly)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                    .padding(bottom = 6.dp)
            ) {
                // Day Filter Chips Row
                DayFilterChipsRow(
                    filters = uiState.availableFilters,
                    selectedFilter = uiState.selectedFilter,
                    onFilterSelected = {
                        viewModel.setDayFilter(it)
                        onScrollToNowChange(true)
                    }
                )

                // Next Event Pill
                if (uiState.nextSession != null) {
                    NextEventPill(
                        session = uiState.nextSession,
                        onNavigateToMap = { session ->
                            onNavigateToMap(session)
                        }
                    )
                }
            }

            // Main Schedule Content: Timeline / Calendar View
            if (uiState.filteredSessions.isEmpty()) {
                EmptyScheduleView(
                    filter = uiState.selectedFilter,
                    onResetFilter = { viewModel.setDayFilter(DayFilter.All) },
                    onAddSession = { onNavigateToConfigure(null) }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("schedule_list"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    groupedByDate.forEach { (date, sessionsForDay) ->
                        item(key = "header_$date") {
                            DayDateHeader(date = date)
                        }

                        items(
                            items = sessionsForDay,
                            key = { it.id }
                        ) { session ->
                            SessionCalendarCard(
                                session = session,
                                onOpenDirections = { openDirections(context, session) },
                                onViewOnMap = { onNavigateToMap(session) },
                                onEdit = { onNavigateToConfigure(session.id) },
                                onToggleStar = { viewModel.setStarred(session, !session.starred) }
                            )
                        }
                    }

                    // Extra bottom space so the target day header can always scroll to the very top of the screen
                    item(key = "bottom_empty_space") {
                        Spacer(modifier = Modifier.height(600.dp))
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = { onNavigateToConfigure(null) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("fab_add_session")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Session")
        }
    }
}

private fun calculateTargetIndex(groupedByDate: Map<String, List<ConferenceSession>>): Int {
    if (groupedByDate.isEmpty()) return 0

    val actualTodayStr = LocalDate.now().toString()

    val sortedDates = groupedByDate.keys.sorted()
    val minDate = sortedDates.first()
    val maxDate = sortedDates.last()

    // If current day is before first day or after last day of conference -> top of list (0)
    if (actualTodayStr < minDate || actualTodayStr > maxDate) {
        return 0
    }

    // Find today's date or the first upcoming conference date
    val targetDate = sortedDates.firstOrNull { it >= actualTodayStr } ?: minDate

    var currentIndex = 0
    for ((date, sessions) in groupedByDate) {
        if (date == targetDate) {
            return currentIndex
        }
        currentIndex += 1 + sessions.size // 1 for header + N for sessions
    }
    return 0
}
