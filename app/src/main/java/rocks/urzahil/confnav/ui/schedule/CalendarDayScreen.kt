package rocks.urzahil.confnav.ui.schedule

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import rocks.urzahil.confnav.data.model.ConferenceSession
import rocks.urzahil.confnav.ui.AgendaLogic
import rocks.urzahil.confnav.ui.CalendarSlot
import rocks.urzahil.confnav.ui.ConferenceViewModel
import rocks.urzahil.confnav.ui.DayFilter
import rocks.urzahil.confnav.ui.components.NextEventPill
import rocks.urzahil.confnav.ui.components.PinColorHelper
import rocks.urzahil.confnav.ui.components.SessionTitleText
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

private val HOUR_HEIGHT = 64.dp
private val TIME_GUTTER_WIDTH = 48.dp
private const val INDENT_FRACTION = 0.12f // horizontal cascade step for overlapping sessions

/**
 * The conference days picked in the day filter on one continuous hourly time grid, like a calendar app. Each day
 * starts with the same date pill as the Agenda, which sticks to the top while scrolling
 * through that day. Overlapping sessions are drawn cascaded on top of each other so every
 * one of them stays visible; starred sessions are indented past the sessions they overlap and drawn above them.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarDayScreen(
    viewModel: ConferenceViewModel,
    onNavigateToMap: (ConferenceSession?) -> Unit,
    onNavigateToConfigure: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    scrollToNow: Boolean = true,
    onScrollToNowChange: (Boolean) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val days = remember(uiState.filteredSessions) {
        uiState.filteredSessions
            .groupBy { it.date }
            .toSortedMap()
            .map { (date, sessions) -> date to AgendaLogic.layoutCalendarDay(sessions) }
            .filter { (_, slots) -> slots.isNotEmpty() }
    }
    val today = LocalDate.now().toString()
    val density = LocalDensity.current

    // When the tab is first opened or on picking a filter (coming back to the tab keeps its
    // position): jump to the current time when today is shown, else to the next shown day;
    // if every shown day is in the past or the future, start at the top.
    LaunchedEffect(scrollToNow, uiState.selectedFilter, uiState.starredOnly, days.isNotEmpty()) {
        if (!scrollToNow || days.isEmpty()) return@LaunchedEffect
        val dayIndex = days.indexOfFirst { (date, _) -> date >= today }
        when {
            today < days.first().first || dayIndex == -1 -> listState.scrollToItem(0)
            days[dayIndex].first == today -> {
                val now = LocalTime.now()
                val offset = with(density) {
                    minuteOffset(now.hour * 60 + now.minute - 60, firstHourOf(days[dayIndex].second)).roundToPx()
                }
                listState.scrollToItem(dayIndex * 2 + 1, offset.coerceAtLeast(0))
            }
            else -> listState.scrollToItem(dayIndex * 2)
        }
        // Only after scrolling: this changes the effect's key, which would cancel it.
        onScrollToNowChange(false)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("calendar_screen")
    ) {
        // Top Bar with Filter Chips & Next Event Pill (matching Agenda and Map)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                .padding(bottom = 6.dp)
        ) {
            DayFilterChipsRow(
                filters = uiState.availableFilters,
                selectedFilter = uiState.selectedFilter,
                onFilterSelected = {
                    viewModel.setDayFilter(it)
                    onScrollToNowChange(true)
                },
                starredOnly = uiState.starredOnly,
                onStarredOnlyChange = {
                    viewModel.setStarredOnly(it)
                    onScrollToNowChange(true)
                }
            )

            if (uiState.nextSession != null) {
                NextEventPill(
                    session = uiState.nextSession,
                    onNavigateToMap = { session -> onNavigateToMap(session) }
                )
            }
        }

        if (days.isEmpty()) {
            EmptyScheduleView(
                filter = uiState.selectedFilter,
                starredOnly = uiState.starredOnly,
                onResetFilter = { viewModel.setDayFilter(DayFilter.All) },
                onShowAllSessions = { viewModel.setStarredOnly(false) },
                onAddSession = { onNavigateToConfigure(null) }
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                days.forEach { (date, slots) ->
                    stickyHeader(key = "header_$date") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            DayDateHeader(date = date)
                        }
                    }
                    item(key = "grid_$date") {
                        CalendarDayGrid(
                            slots = slots,
                            isToday = date == today,
                            onSessionClick = { onNavigateToConfigure(it) },
                            modifier = Modifier.testTag("calendar_grid_$date")
                        )
                    }
                }
            }
        }
    }
}

/** The hour a day's grid starts at: one hour before its first session. */
private fun firstHourOf(slots: List<CalendarSlot>): Int =
    (slots.minOf { it.startMinute } / 60 - 1).coerceAtLeast(0)

private fun minuteOffset(minute: Int, firstHour: Int): Dp =
    HOUR_HEIGHT * ((minute - firstHour * 60) / 60f)

@Composable
private fun CalendarDayGrid(
    slots: List<CalendarSlot>,
    isToday: Boolean,
    onSessionClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val firstHour = firstHourOf(slots)
    val lastHour = ((slots.maxOf { it.endMinute } + 59) / 60 + 1).coerceAtMost(24)
    val hourCount = lastHour - firstHour

    fun minuteToY(minute: Int) = minuteOffset(minute, firstHour)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 16.dp)
    ) {
        // Hour labels
        Box(
            modifier = Modifier
                .width(TIME_GUTTER_WIDTH)
                .height(HOUR_HEIGHT * hourCount)
        ) {
            for (i in 1 until hourCount) {
                Text(
                    text = String.format(Locale.US, "%02d:00", firstHour + i),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(y = HOUR_HEIGHT * i - 7.dp)
                        .padding(end = 6.dp)
                )
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(HOUR_HEIGHT * hourCount)
        ) {
            val lineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            Box(
                Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(lineColor)
            )
            for (i in 0 until hourCount) {
                Box(
                    Modifier
                        .offset(y = HOUR_HEIGHT * i)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(lineColor)
                )
            }

            slots.forEach { slot ->
                val indent = (slot.column * INDENT_FRACTION).coerceAtMost(1f - INDENT_FRACTION * 2)
                val top = minuteToY(slot.startMinute)
                val height = (minuteToY(slot.endMinute) - top - 2.dp).coerceAtLeast(20.dp)
                CalendarSessionBlock(
                    slot = slot,
                    compact = height < 48.dp,
                    modifier = Modifier
                        .zIndex(if (slot.session.starred) 1f else 0f)
                        .offset(x = maxWidth * indent, y = top)
                        .width(maxWidth * (1f - indent))
                        .height(height)
                        .clickable { onSessionClick(slot.session.id) }
                )
            }

            if (isToday) {
                val now = LocalTime.now()
                val nowMinute = now.hour * 60 + now.minute
                if (nowMinute in firstHour * 60..lastHour * 60) {
                    val y = minuteToY(nowMinute)
                    Box(
                        Modifier
                            .zIndex(2f)
                            .offset(y = y - 1.dp)
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(MaterialTheme.colorScheme.error)
                    )
                    Box(
                        Modifier
                            .zIndex(2f)
                            .offset(x = (-4).dp, y = y - 5.dp)
                            .size(10.dp)
                            .background(MaterialTheme.colorScheme.error, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarSessionBlock(
    slot: CalendarSlot,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val session = slot.session
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier = modifier
            .background(PinColorHelper.parseColor(session.colorHex), shape)
            .border(1.dp, MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), shape)
            .padding(horizontal = 8.dp, vertical = if (compact) 2.dp else 4.dp)
            .testTag("calendar_session_${session.id}")
    ) {
        if (compact) {
            SessionTitleText(
                session = session,
                text = "${session.title}, ${session.startTime} · ${session.locationName}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            SessionTitleText(
                session = session,
                style = MaterialTheme.typography.labelLarge.copy(lineHeight = 16.sp),
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${session.startTime} – ${session.endTime}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.9f),
                maxLines = 1
            )
            Text(
                text = session.locationName,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
