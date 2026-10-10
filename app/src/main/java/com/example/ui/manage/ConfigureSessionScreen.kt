package com.example.ui.manage

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ConferenceSession
import com.example.data.model.SessionJsonHelper
import com.example.ui.ConferenceViewModel
import com.example.ui.components.PinColorHelper
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigureSessionScreen(
    viewModel: ConferenceViewModel,
    editingSessionId: Long? = null,
    onSessionSaved: () -> Unit = {},
    onEditCancelled: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current

    var selectedTab by remember { mutableIntStateOf(if (editingSessionId != null) 0 else 0) }

    // Form fields state
    var currentId by remember { mutableStateOf(editingSessionId ?: 0L) }
    var title by remember { mutableStateOf("") }
    var speaker by remember { mutableStateOf("") }
    var speakerRole by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var locationName by remember { mutableStateOf("") }
    var locationAddress by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf(0.0) }
    var longitude by remember { mutableStateOf(0.0) }
    var description by remember { mutableStateOf("") }

    // Calendar DatePicker Dialog State
    var showDatePickerDialog by remember { mutableStateOf(false) }

    // Autocomplete search query
    var locationQuery by remember { mutableStateOf("") }
    var showSuggestions by remember { mutableStateOf(false) }

    // Dialog state for delete confirmation
    var sessionToDelete by remember { mutableStateOf<ConferenceSession?>(null) }

    // Export / Import JSON State
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
                            // Agenda is currently empty: import directly!
                            viewModel.importSessions(parsedSessions, replaceAll = true) { count ->
                                Toast.makeText(
                                    context,
                                    "Successfully imported $count sessions!",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            // Agenda already has sessions: prompt to add or replace
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

    // Prepopulate when editing
    LaunchedEffect(editingSessionId, uiState.sessions) {
        if (editingSessionId != null && editingSessionId != 0L) {
            val existing = uiState.sessions.firstOrNull { it.id == editingSessionId }
            if (existing != null) {
                currentId = existing.id
                title = existing.title
                speaker = existing.speaker
                speakerRole = existing.speakerRole
                selectedDate = existing.date
                startTime = existing.startTime
                endTime = existing.endTime
                locationName = existing.locationName
                locationAddress = existing.address
                latitude = existing.latitude
                longitude = existing.longitude
                description = existing.description
                locationQuery = existing.locationName
            }
        }
    }

    // Dynamic list of unique conference dates from all existing sessions
    val existingConferenceDates = remember(uiState.sessions) {
        uiState.sessions.map { it.date }.distinct().sorted()
    }

    // Formatted date string for user-friendly display
    val formattedSelectedDate = remember(selectedDate) {
        try {
            val parsed = LocalDate.parse(selectedDate)
            parsed.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.UK))
        } catch (_: Exception) {
            selectedDate
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("configure_session_screen")
    ) {
        // Tabs: "Form" vs "Manage All (N)"
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        text = if (currentId != 0L) "Edit Session" else "Add Session",
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        text = "All Sessions (${uiState.sessions.size})",
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }

        if (selectedTab == 0) {
            // ==================== SESSION CONFIGURATION FORM ====================
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("configure_session_form"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Title field
                item {
                    SessionTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = "Session Title *",
                        leadingIcon = {
                            Icon(
                                Icons.Default.Title,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_session_title")
                    )
                }

                // Speaker and Role
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SessionTextField(
                            value = speaker,
                            onValueChange = { speaker = it },
                            label = "Speaker",
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_session_speaker")
                        )
                        SessionTextField(
                            value = speakerRole,
                            onValueChange = { speakerRole = it },
                            label = "Role / Co.",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // ==================== CALENDAR DATE SELECTION ====================
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Session Date *",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Interactive Date Card triggering Calendar
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showDatePickerDialog = true }
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    RoundedCornerShape(10.dp)
                                )
                                .testTag("date_picker_card"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = "Pick Date",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = formattedSelectedDate,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Tap to choose any date from calendar",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.5.sp
                                    )
                                }
                            }
                        }

                        // Dynamic shortcut chips for existing conference dates
                        if (existingConferenceDates.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                items(existingConferenceDates) { dateStr ->
                                    val isSelected = selectedDate == dateStr
                                    val shortLabel = try {
                                        val d = LocalDate.parse(dateStr)
                                        d.format(DateTimeFormatter.ofPattern("d MMM", Locale.UK))
                                    } catch (_: Exception) {
                                        dateStr
                                    }

                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedDate = dateStr },
                                        label = { Text(shortLabel, fontSize = 11.5.sp) },
                                        leadingIcon = if (isSelected) {
                                            {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        } else null,
                                        modifier = Modifier.height(28.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Time Range (Start & End)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SessionTextField(
                            value = startTime,
                            onValueChange = { startTime = it },
                            label = "Start Time *",
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_start_time")
                        )
                        SessionTextField(
                            value = endTime,
                            onValueChange = { endTime = it },
                            label = "End Time *",
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_end_time")
                        )
                    }
                }

                // ==================== GOOGLE MAPS AUTOCOMPLETE LOCATION ====================
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Location (Google Maps Autocomplete) *",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Autocomplete Search Input
                        SessionTextField(
                            value = locationQuery,
                            onValueChange = {
                                locationQuery = it
                                showSuggestions = true
                                viewModel.searchLocations(it)
                            },
                            label = "Search location",
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (uiState.isSearchingLocations) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else if (locationQuery.isNotEmpty()) {
                                    IconButton(onClick = {
                                        locationQuery = ""
                                        showSuggestions = false
                                    }, modifier = Modifier.size(24.dp)) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_location_search")
                        )

                        // Autocomplete Suggestions Dropdown/Card
                        AnimatedVisibility(visible = showSuggestions && uiState.locationSuggestions.isNotEmpty()) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                tonalElevation = 4.dp,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Column(modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp)) {
                                    uiState.locationSuggestions.take(5).forEach { suggestion ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    // Retain descriptive venue name rather than just replacing with a street address
                                                    val isSuggestionAddressOnly =
                                                        suggestion.name.matches(Regex("^[0-9]+ .*")) ||
                                                                suggestion.name.matches(Regex("^[0-9]+[A-Za-z]?,? .*"))

                                                    val chosenVenueName = when {
                                                        !isSuggestionAddressOnly -> suggestion.name
                                                        locationName.isNotBlank() && !locationName.matches(
                                                            Regex("^[0-9]+ .*")
                                                        ) -> locationName

                                                        locationQuery.isNotBlank() && !locationQuery.matches(
                                                            Regex("^[0-9]+ .*")
                                                        ) -> locationQuery.split(",").first().trim()

                                                        else -> suggestion.name
                                                    }

                                                    locationName = chosenVenueName
                                                    locationAddress =
                                                        suggestion.address.ifBlank { suggestion.name }
                                                    locationQuery = chosenVenueName
                                                    showSuggestions = false
                                                    keyboardController?.hide()
                                                    focusManager.clearFocus(force = true)

                                                    // Resolve coordinates only for the selected prediction.
                                                    if (!suggestion.placeId.isNullOrBlank()) {
                                                        viewModel.resolveLocationSuggestion(
                                                            suggestion.placeId
                                                        ) { coordinates ->
                                                            if (coordinates != null) {
                                                                latitude = coordinates.first
                                                                longitude = coordinates.second
                                                            }
                                                        }
                                                    } else {
                                                        latitude = suggestion.latitude
                                                        longitude = suggestion.longitude
                                                    }
                                                }
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = suggestion.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                if (suggestion.address.isNotBlank()) {
                                                    Text(
                                                        text = suggestion.address,
                                                        style = MaterialTheme.typography.bodySmall,
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

                        // Descriptive Venue / Room Name Field
                        Spacer(modifier = Modifier.height(10.dp))
                        val venueColorHex = remember(locationName, uiState.sessions) {
                            viewModel.getOrAssignVenueColor(locationName, currentId)
                        }
                        val resolvedColor = PinColorHelper.parseColor(venueColorHex)

                        SessionTextField(
                            value = locationName,
                            onValueChange = { locationName = it },
                            label = "Venue / Location Name *",
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = if (locationName.isNotBlank()) resolvedColor else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_location_name")
                        )

                        // Confirmed Location Summary Card
                        if (locationName.isNotBlank() || locationAddress.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val isExistingVenue = remember(locationName, uiState.sessions) {
                                val clean = locationName.trim()
                                clean.isNotBlank() && uiState.sessions.any {
                                    it.id != currentId && it.locationName.trim()
                                        .equals(clean, ignoreCase = true)
                                }
                            }
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        resolvedColor.copy(alpha = 0.5f),
                                        RoundedCornerShape(12.dp)
                                    ),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(resolvedColor)
                                            .border(2.dp, Color.White, CircleShape)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = locationName.ifBlank { "Venue" },
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (isExistingVenue)
                                                "Existing venue • Pin color reused"
                                            else
                                                "New venue • Pin color auto-assigned",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (locationAddress.isNotBlank()) {
                                            Text(
                                                text = "Address: $locationAddress",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Notes / Description
                item {
                    SessionTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = "Session Notes / Description",
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Action Buttons (Save, Clear)
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (currentId != 0L) {
                            OutlinedButton(
                                onClick = {
                                    // Return to the screen that invoked this edit.
                                    currentId = 0L
                                    title = ""
                                    speaker = ""
                                    speakerRole = ""
                                    locationName = ""
                                    locationAddress = ""
                                    locationQuery = ""
                                    description = ""
                                    onEditCancelled()
                                    selectedTab = 1
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Cancel Edit")
                            }
                        }

                        Button(
                            onClick = {
                                // Validation
                                if (title.isBlank()) {
                                    Toast.makeText(
                                        context,
                                        "Please enter a session title",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@Button
                                }
                                if (locationName.isBlank()) {
                                    // Fallback location if user didn't pick from autocomplete
                                    locationName =
                                        if (locationQuery.isNotBlank()) locationQuery else "Conference Hall"
                                    locationAddress = "Conference Center Campus"
                                }

                                val session = ConferenceSession(
                                    id = currentId,
                                    title = title.trim(),
                                    speaker = speaker.trim(),
                                    speakerRole = speakerRole.trim(),
                                    date = selectedDate,
                                    startTime = startTime.trim(),
                                    endTime = endTime.trim(),
                                    locationName = locationName.trim(),
                                    address = locationAddress.trim(),
                                    latitude = latitude,
                                    longitude = longitude,
                                    colorHex = viewModel.getOrAssignVenueColor(
                                        locationName.trim(),
                                        currentId
                                    ),
                                    track = "",
                                    description = description.trim()
                                )

                                viewModel.saveSession(session) {
                                    Toast.makeText(
                                        context,
                                        if (currentId != 0L) "Session updated!" else "Session added to agenda!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    // Reset form
                                    currentId = 0L
                                    title = ""
                                    speaker = ""
                                    speakerRole = ""
                                    locationName = ""
                                    locationAddress = ""
                                    locationQuery = ""
                                    description = ""
                                    onSessionSaved()
                                    selectedTab = 1
                                }
                            },
                            modifier = Modifier
                                .weight(1.5f)
                                .height(52.dp)
                                .testTag("btn_save_session"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (currentId != 0L) "Update Session" else "Save Session",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        } else {
            // ==================== ALL SESSIONS MANAGEMENT LIST ====================
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("manage_sessions_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Configured Sessions (${uiState.sessions.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(
                                onClick = {
                                    currentId = 0L
                                    title = ""
                                    speaker = ""
                                    speakerRole = ""
                                    locationName = ""
                                    locationAddress = ""
                                    locationQuery = ""
                                    description = ""
                                    selectedTab = 0
                                },
                                modifier = Modifier.testTag("btn_new_session_from_tab")
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // JSON Export & Import Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (uiState.sessions.isEmpty()) {
                                        Toast.makeText(
                                            context,
                                            "No sessions to export. Add sessions first.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        exportDocumentLauncher.launch("conference_sessions.json")
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_export_json")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = "Export JSON",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export JSON", maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = {
                                    // Go straight to loading a file
                                    jsonFilePickerLauncher.launch(
                                        arrayOf(
                                            "application/json",
                                            "text/*",
                                            "*/*"
                                        )
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_import_json")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = "Import JSON",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Import JSON", maxLines = 1)
                            }
                        }
                    }
                }

                items(uiState.sessions, key = { it.id }) { session ->
                    val locationColor = PinColorHelper.parseColor(session.colorHex)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                1.dp,
                                locationColor.copy(alpha = 0.35f),
                                RoundedCornerShape(14.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(locationColor)
                                )
                                Column {
                                    Text(
                                        text = session.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${ConferenceViewModel.formatEuropeanDate(session.date)} • ${session.startTime} • ${session.locationName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row {
                                IconButton(
                                    onClick = {
                                        currentId = session.id
                                        title = session.title
                                        speaker = session.speaker
                                        speakerRole = session.speakerRole
                                        selectedDate = session.date
                                        startTime = session.startTime
                                        endTime = session.endTime
                                        locationName = session.locationName
                                        locationAddress = session.address
                                        latitude = session.latitude
                                        longitude = session.longitude
                                        description = session.description
                                        locationQuery = session.locationName
                                        selectedTab = 0
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                IconButton(
                                    onClick = { sessionToDelete = session }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ==================== CALENDAR DATE PICKER DIALOG ====================
    if (showDatePickerDialog) {
        key(selectedDate) {
            val initialMillis = remember(selectedDate) {
                try {
                    val localDate = LocalDate.parse(selectedDate)
                    localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                } catch (_: Exception) {
                    System.currentTimeMillis()
                }
            }
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = initialMillis
            )

            DatePickerDialog(
                onDismissRequest = { showDatePickerDialog = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val pickedDate = Instant.ofEpochMilli(millis)
                                    .atZone(ZoneOffset.UTC)
                                    .toLocalDate()
                                selectedDate = pickedDate.toString()
                            }
                            showDatePickerDialog = false
                        }
                    ) {
                        Text("Select Date", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePickerDialog = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(
                    state = datePickerState,
                    title = {
                        Text(
                            text = "Select date",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp, bottom = 4.dp)
                        )
                    },
                    headline = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        val formattedHeadline = if (selectedMillis != null) {
                            try {
                                val pickedDate = Instant.ofEpochMilli(selectedMillis)
                                    .atZone(ZoneOffset.UTC)
                                    .toLocalDate()
                                pickedDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK))
                            } catch (_: Exception) {
                                selectedDate
                            }
                        } else {
                            "Select date"
                        }
                        Text(
                            text = formattedHeadline,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 12.dp)
                        )
                    }
                )
            }
        }
    }

    // Delete Confirmation Dialog
    val toDelete = sessionToDelete
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Delete Session?") },
            text = { Text("Are you sure you want to remove '${toDelete.title}' from your agenda?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSession(toDelete.id)
                        sessionToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ==================== IMPORT SESSIONS OPTIONS DIALOG ====================
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
                        viewModel.importSessions(importBatch, replaceAll = false) { importedCount ->
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
