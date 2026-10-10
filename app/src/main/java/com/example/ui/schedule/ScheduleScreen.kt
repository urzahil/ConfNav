package com.example.ui.schedule

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ConferenceSession
import com.example.data.model.SessionJsonHelper
import com.example.ui.ConferenceViewModel
import com.example.ui.DayFilter
import com.example.ui.components.NextEventPill
import com.example.ui.components.PinColorHelper
import com.example.ui.components.openDirections
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: ConferenceViewModel,
    onNavigateToMap: (ConferenceSession?) -> Unit,
    onNavigateToConfigure: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var pendingImportSessions by remember { mutableStateOf<List<ConferenceSession>?>(null) }

    // Direct JSON file save launcher for Export
    val exportDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val jsonBytes = viewModel.exportSessionsJson().toByteArray(Charsets.UTF_8)
                    outputStream.write(jsonBytes)
                    outputStream.flush()
                }
                Toast.makeText(
                    context,
                    "Saved conference_sessions.json successfully!",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save file: ${e.message}", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    // Direct JSON file picker launcher for Import (goes straight to file loading)
    val jsonFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val jsonText = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader().readText()
                }.orEmpty()

                if (jsonText.isBlank()) {
                    Toast.makeText(context, "Selected file is empty", Toast.LENGTH_SHORT).show()
                } else {
                    val parseResult = SessionJsonHelper.importFromJson(jsonText)
                    if (parseResult.isFailure) {
                        val errorMsg =
                            parseResult.exceptionOrNull()?.message ?: "Invalid JSON syntax"
                        Toast.makeText(context, "Import failed: $errorMsg", Toast.LENGTH_LONG)
                            .show()
                    } else {
                        val parsedSessions = parseResult.getOrThrow()
                        if (parsedSessions.isEmpty()) {
                            Toast.makeText(
                                context,
                                "No valid sessions found in JSON file",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else if (uiState.sessions.isEmpty()) {
                            viewModel.importSessions(parsedSessions, replaceAll = true) { count ->
                                Toast.makeText(
                                    context,
                                    "Successfully imported $count sessions!",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            pendingImportSessions = parsedSessions
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read file: ${e.message}", Toast.LENGTH_LONG)
                    .show()
            }
        }
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
                    onFilterSelected = { viewModel.setDayFilter(it) }
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
                val groupedByDate = uiState.filteredSessions.groupBy { it.date }

                LazyColumn(
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
                                onEdit = { onNavigateToConfigure(session.id) }
                            )
                        }
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

        // Dialog for import options when current agenda already has sessions
        val importBatch = pendingImportSessions
        if (importBatch != null) {
            val count = importBatch.size
            AlertDialog(
                onDismissRequest = { pendingImportSessions = null },
                icon = {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = { Text("Import $count Sessions") },
                text = {
                    Text(
                        "Found $count sessions in the loaded file.\n\nYou currently have ${uiState.sessions.size} session(s) in your agenda. Would you like to add these to your existing agenda or replace everything?"
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.importSessions(
                                importBatch,
                                replaceAll = false
                            ) { importedCount ->
                                Toast.makeText(
                                    context,
                                    "Added $importedCount sessions to agenda!",
                                    Toast.LENGTH_LONG
                                ).show()
                                pendingImportSessions = null
                            }
                        }
                    ) {
                        Text("Add to Agenda")
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { pendingImportSessions = null }) {
                            Text("Cancel")
                        }
                        TextButton(
                            onClick = {
                                viewModel.importSessions(
                                    importBatch,
                                    replaceAll = true
                                ) { importedCount ->
                                    Toast.makeText(
                                        context,
                                        "Replaced agenda with $importedCount sessions!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    pendingImportSessions = null
                                }
                            }
                        ) {
                            Text("Replace All", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun DayFilterChipsRow(
    filters: List<DayFilter>,
    selectedFilter: DayFilter,
    onFilterSelected: (DayFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .testTag("day_filter_row"),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(filters) { filter ->
            val isSelected = when {
                filter is DayFilter.All && selectedFilter is DayFilter.All -> true
                filter is DayFilter.Today && selectedFilter is DayFilter.Today -> true
                filter is DayFilter.SpecificDate && selectedFilter is DayFilter.SpecificDate ->
                    filter.date == selectedFilter.date

                else -> false
            }

            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        text = filter.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .testTag("filter_chip_${filter.label.replace(" ", "_")}")
            )
        }
    }
}

@Composable
fun DayDateHeader(date: String) {
    val formatted = try {
        val parsed = LocalDate.parse(date)
        parsed.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.UK))
    } catch (_: Exception) {
        date
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.padding(end = 8.dp)
        ) {
            Text(
                text = formatted,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        )
    }
}

@Composable
fun SessionCalendarCard(
    session: ConferenceSession,
    onOpenDirections: () -> Unit,
    onViewOnMap: () -> Unit,
    onEdit: () -> Unit,
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
                                .clickable { onViewOnMap() }
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
                    Text(
                        text = session.title,
                        style = MaterialTheme.typography.titleSmall,
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
                            OutlinedButton(
                                onClick = onOpenDirections,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .height(28.dp)
                                    .testTag("session_directions_${session.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Directions,
                                    contentDescription = "Directions",
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
                            OutlinedButton(
                                onClick = onViewOnMap,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = "Map",
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

@Composable
fun EmptyScheduleView(
    filter: DayFilter,
    onResetFilter: () -> Unit,
    onAddSession: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )

            Text(
                text = if (filter is DayFilter.All) "No sessions scheduled yet" else "No sessions for ${filter.label}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = if (filter is DayFilter.All)
                    "Get started by adding conference sessions to your agenda."
                else
                    "Try switching the filter to All Days or add a new session.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (filter !is DayFilter.All) {
                    OutlinedButton(onClick = onResetFilter) {
                        Text("Show All Days")
                    }
                }
                androidx.compose.material3.Button(onClick = onAddSession) {
                    Text("Add Session")
                }
            }
        }
    }
}
