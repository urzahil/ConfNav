package rocks.urzahil.confnav.ui.manage

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.SecondaryTabRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import rocks.urzahil.confnav.data.model.ConferenceSession
import rocks.urzahil.confnav.data.model.SessionJsonHelper
import rocks.urzahil.confnav.ui.ConferenceViewModel
import rocks.urzahil.confnav.ui.components.PinColorHelper
import rocks.urzahil.confnav.ui.icons.filled.CalendarMonth
import rocks.urzahil.confnav.ui.icons.filled.Schedule
import rocks.urzahil.confnav.ui.icons.filled.Title
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

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
    val locale = LocalLocale.current.platformLocale

    var selectedTab by remember { mutableIntStateOf(0) }

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

    // Field the form does not show but must keep when a session is edited
    var track by remember { mutableStateOf("") }

    fun clearForm() {
        currentId = 0L
        title = ""
        speaker = ""
        speakerRole = ""
        startTime = ""
        endTime = ""
        locationName = ""
        locationAddress = ""
        locationQuery = ""
        latitude = 0.0
        longitude = 0.0
        description = ""
        track = ""
    }

    fun loadSession(session: ConferenceSession) {
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
        track = session.track
    }

    // JSON export / import (file pickers, add-or-replace dialog and result messages)
    val importExport = rememberSessionImportExport(viewModel, uiState.sessions.size)

    // Fill the form once when an edit starts. It is keyed on the session being present rather than on
    // the whole list, so unrelated database changes never overwrite what the user is typing.
    val sessionBeingEdited = uiState.sessions.firstOrNull { it.id == editingSessionId }
    LaunchedEffect(editingSessionId, sessionBeingEdited != null) {
        if (editingSessionId != null && editingSessionId != 0L && sessionBeingEdited != null) {
            loadSession(sessionBeingEdited)
        }
    }

    // Dynamic list of unique conference dates from all existing sessions
    val existingConferenceDates = remember(uiState.sessions) {
        uiState.sessions.map { it.date }.distinct().sorted()
    }

    // Formatted date string for user-friendly display
    val formattedSelectedDate = remember(selectedDate, locale) {
        try {
            val parsed = LocalDate.parse(selectedDate)
            parsed.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", locale))
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
        SecondaryTabRow(
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
                                        d.format(DateTimeFormatter.ofPattern("d MMM", locale))
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
                                border = BorderStroke(
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
                                    clearForm()
                                    onEditCancelled()
                                    selectedTab = 1
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Cancel")
                            }

                            Button(
                                onClick = {
                                    sessionToDelete = ConferenceSession(
                                        id = currentId,
                                        title = title.trim().ifBlank { "this session" },
                                        speaker = speaker,
                                        date = selectedDate,
                                        startTime = startTime,
                                        endTime = endTime,
                                        locationName = locationName,
                                        address = locationAddress,
                                        latitude = latitude,
                                        longitude = longitude,
                                        colorHex = ""
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_delete_session"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = Color.Black
                                )
                            ) {
                                Text("Delete")
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
                                val parsedStart = SessionJsonHelper.parseTime(startTime)
                                val parsedEnd = SessionJsonHelper.parseTime(endTime)
                                if (parsedStart == null || parsedEnd == null) {
                                    Toast.makeText(
                                        context,
                                        "Enter start and end times as 24-hour HH:mm, e.g. 09:30",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@Button
                                }
                                if (!parsedEnd.isAfter(parsedStart)) {
                                    Toast.makeText(
                                        context,
                                        "End time must be after the start time",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@Button
                                }
                                if (locationName.isBlank()) {
                                    // Allow a typed venue name that wasn't picked from autocomplete.
                                    if (locationQuery.isBlank()) {
                                        Toast.makeText(
                                            context,
                                            "Please choose or enter a location",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@Button
                                    }
                                    locationName = locationQuery.trim()
                                }
                                val hasCoordinates = !(latitude == 0.0 && longitude == 0.0)

                                val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
                                val session = ConferenceSession(
                                    id = currentId,
                                    title = title.trim(),
                                    speaker = speaker.trim(),
                                    speakerRole = speakerRole.trim(),
                                    date = selectedDate,
                                    startTime = parsedStart.format(timeFormat),
                                    endTime = parsedEnd.format(timeFormat),
                                    locationName = locationName.trim(),
                                    address = locationAddress.trim(),
                                    latitude = latitude,
                                    longitude = longitude,
                                    colorHex = "", // assigned by saveSession from the venue
                                    track = track,
                                    description = description.trim()
                                )

                                val wasEdit = currentId != 0L
                                viewModel.saveSession(session) {
                                    val saved = if (wasEdit) "Session updated!" else "Session added to agenda!"
                                    val note = if (hasCoordinates) "" else
                                        " No coordinates were found, so it won't appear on the map."
                                    Toast.makeText(
                                        context,
                                        saved + note,
                                        if (hasCoordinates) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
                                    ).show()
                                    // Reset form
                                    clearForm()
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
                                text = "Save",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        } else {
            // ==================== ALL SESSIONS MANAGEMENT LIST ====================
            ManageSessionsList(
                sessions = uiState.sessions,
                importExport = importExport,
                onNew = {
                    clearForm()
                    selectedTab = 0
                },
                onEdit = { session ->
                    loadSession(session)
                    selectedTab = 0
                },
                onDelete = { sessionToDelete = it }
            )
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
                                pickedDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy", locale))
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
                        if (toDelete.id == currentId) {
                            // Deleted from the edit form: leave the form like Cancel does.
                            clearForm()
                            onEditCancelled()
                            selectedTab = 1
                        }
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
}
