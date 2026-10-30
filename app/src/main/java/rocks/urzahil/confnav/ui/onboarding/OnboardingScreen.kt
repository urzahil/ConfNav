package rocks.urzahil.confnav.ui.onboarding

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import rocks.urzahil.confnav.ui.icons.filled.CalendarMonth
import rocks.urzahil.confnav.ui.icons.filled.Directions
import rocks.urzahil.confnav.ui.icons.filled.EditCalendar
import rocks.urzahil.confnav.ui.icons.filled.FileDownload
import rocks.urzahil.confnav.ui.icons.filled.Map
import rocks.urzahil.confnav.ui.icons.filled.Watch

const val WEB_BUILDER_URL = "https://urzahil.github.io/ConfNav"

private data class OnboardingPoint(val icon: ImageVector, val title: String, val text: String)

private data class OnboardingSlide(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val points: List<OnboardingPoint>,
    val opensWebBuilder: Boolean = false
)

private val slides = listOf(
    OnboardingSlide(
        icon = Icons.Filled.CalendarMonth,
        title = "Welcome to ConfNav",
        subtitle = "Your conference agenda, venues and walking directions in one place, even offline.",
        points = listOf(
            OnboardingPoint(Icons.Filled.CalendarMonth, "Agenda", "Browse each day's sessions and star the ones you don't want to miss."),
            OnboardingPoint(Icons.Filled.DateRange, "Calendar", "See the day as a timeline, with overlapping sessions side by side."),
            OnboardingPoint(Icons.Filled.Map, "Map", "Every venue as a colour-coded pin, with its sessions grouped together.")
        )
    ),
    OnboardingSlide(
        icon = Icons.Filled.Create,
        title = "Build it on the web",
        subtitle = "The fastest way to create sessions is the free web builder, on any computer.",
        points = listOf(
            OnboardingPoint(Icons.Filled.EditCalendar, "1. Add sessions", "Enter title, speaker, time and colour for each session."),
            OnboardingPoint(Icons.Filled.Place, "2. Find the venue", "Look up the address and the builder places it on the map."),
            OnboardingPoint(Icons.Filled.FileDownload, "3. Export JSON", "Download the agenda file and send it to your phone.")
        ),
        opensWebBuilder = true
    ),
    OnboardingSlide(
        icon = Icons.Filled.EditCalendar,
        title = "Import and go",
        subtitle = "Load the agenda in the Configure tab, then let ConfNav guide you through the day.",
        points = listOf(
            OnboardingPoint(Icons.Filled.FileDownload, "Import JSON", "Add the sessions to your agenda, or replace it. Re-importing never creates duplicates."),
            OnboardingPoint(Icons.Filled.Directions, "Up next", "Star key sessions and tap the next-event pill for walking directions."),
            OnboardingPoint(Icons.Filled.Edit, "Edit anytime", "Add, change or delete sessions right in the app.")
        )
    ),
    OnboardingSlide(
        icon = Icons.Filled.Watch,
        title = "On your wrist",
        subtitle = "ConfNav is also available for Wear OS watches.",
        points = listOf(
            OnboardingPoint(Icons.Filled.Watch, "Up next tile", "Your next session's time, title and venue, updated as the day goes on."),
            OnboardingPoint(Icons.Filled.CalendarMonth, "Complication", "The next start time and venue right on your watch face."),
            OnboardingPoint(Icons.Filled.Directions, "Directions", "Tap for walking directions. The agenda syncs from your phone and works offline.")
        )
    )
)

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == slides.lastIndex

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("onboarding_screen"),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            // Fixed height so the slides don't shift when Skip hides on the last one.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isLastPage) {
                    TextButton(
                        onClick = onFinished,
                        modifier = Modifier.testTag("onboarding_skip")
                    ) {
                        Text("Skip")
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                OnboardingSlideContent(
                    slide = slides[page],
                    onOpenWebBuilder = { openWebBuilder(context) }
                )
            }

            PageIndicator(
                pageCount = slides.size,
                currentPage = pagerState.currentPage,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 16.dp)
            )

            Button(
                onClick = {
                    if (isLastPage) {
                        onFinished()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                    .height(52.dp)
                    .testTag("onboarding_next")
            ) {
                Text(
                    text = if (isLastPage) "Get Started" else "Next",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun OnboardingSlideContent(
    slide: OnboardingSlide,
    onOpenWebBuilder: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = slide.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = slide.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = slide.subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            slide.points.forEach { point ->
                OnboardingPointCard(point)
            }
        }

        if (slide.opensWebBuilder) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onOpenWebBuilder,
                modifier = Modifier.testTag("onboarding_open_web_builder")
            ) {
                Text("Open urzahil.github.io/ConfNav", maxLines = 1)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun OnboardingPointCard(point: OnboardingPoint) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = point.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = point.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = point.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PageIndicator(pageCount: Int, currentPage: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            val width by animateDpAsState(if (selected) 24.dp else 8.dp, label = "indicator_width")
            val color by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                label = "indicator_color"
            )
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

private fun openWebBuilder(context: Context) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(WEB_BUILDER_URL)))
    } catch (_: ActivityNotFoundException) {
        // No browser installed: nothing to open.
    }
}
