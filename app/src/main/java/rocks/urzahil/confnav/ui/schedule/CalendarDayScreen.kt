package rocks.urzahil.confnav.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import rocks.urzahil.confnav.ui.AgendaLogic
import rocks.urzahil.confnav.ui.CalendarSlot
import rocks.urzahil.confnav.ui.ConferenceViewModel
import rocks.urzahil.confnav.ui.DayFilter
import rocks.urzahil.confnav.ui.components.PinColorHelper
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

private val HOUR_HEIGHT = 64.dp
private val TIME_GUTTER_WIDTH = 48.dp
private const val INDENT_FRACTION = 0.12f // horizontal cascade step for overlapping sessions

/**
 * Day view on an hourly time grid, like a calendar app: overlapping sessions are drawn
 * cascaded on top of each other so every one of them stays visible.
 */
@Composable
fun CalendarDayScreen(
    viewModel: ConferenceViewModel,
    onNavigateToConfigure: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dayFilters = remember(uiState.availableFilters) {
        uiState.availableFilters.filterIsInstance<DayFilter.SpecificDate>()
    }
    var pickedDate by rememberSaveable { mutableStateOf<String?>(null) }

    // Fall back to the day picked in the Agenda, else today if it is a conference day, else the first day.
    val today = LocalDate.now().toString()
    val activeFilter = dayFilters.firstOrNull { it.date == pickedDate }
        ?: dayFilters.firstOrNull { it == uiState.selectedFilter }
        ?: dayFilters.firstOrNull { it.date == today }
        ?: dayFilters.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("calendar_screen")
    ) {
        if (activeFilter == null) {
            EmptyScheduleView(
                filter = DayFilter.All,
                onResetFilter = {},
                onAddSession = { onNavigateToConfigure(null) }
            )
        } else {
            DayFilterChipsRow(
                filters = dayFilters,
                selectedFilter = activeFilter,
                onFilterSelected = { pickedDate = (it as? DayFilter.SpecificDate)?.date },
                modifier = Modifier.padding(bottom = 6.dp)
            )

            val slots = remember(uiState.sessions, activeFilter.date) {
                AgendaLogic.layoutCalendarDay(uiState.sessions.filter { it.date == activeFilter.date })
            }
            CalendarDayGrid(
                slots = slots,
                isToday = activeFilter.date == today,
                onSessionClick = { onNavigateToConfigure(it) }
            )
        }
    }
}

@Composable
private fun CalendarDayGrid(
    slots: List<CalendarSlot>,
    isToday: Boolean,
    onSessionClick: (Long) -> Unit
) {
    if (slots.isEmpty()) return

    val firstHour = (slots.minOf { it.startMinute } / 60 - 1).coerceAtLeast(0)
    val lastHour = ((slots.maxOf { it.endMinute } + 59) / 60 + 1).coerceAtMost(24)
    val hourCount = lastHour - firstHour
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    fun minuteToY(minute: Int) = HOUR_HEIGHT * ((minute - firstHour * 60) / 60f)

    // Open on the current time when viewing today, otherwise on the first session.
    LaunchedEffect(firstHour, isToday) {
        val now = LocalTime.now()
        val focusMinute = if (isToday) now.hour * 60 + now.minute - 60 else slots.minOf { it.startMinute }
        scrollState.scrollTo(with(density) { minuteToY(focusMinute).roundToPx() }.coerceAtLeast(0))
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 24.dp)
            .testTag("calendar_grid")
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
                            .offset(y = y - 1.dp)
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(MaterialTheme.colorScheme.error)
                    )
                    Box(
                        Modifier
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
            Text(
                text = "${session.title}, ${session.startTime} · ${session.locationName}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            Text(
                text = session.title,
                style = MaterialTheme.typography.labelLarge,
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
