package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.ConferenceSession
import com.example.data.model.LocationGroup
import com.example.data.model.SessionJsonHelper
import com.example.data.network.LocationSearchHelper
import com.example.data.network.LocationSuggestion
import com.example.data.repository.ConferenceRepository
import com.example.ui.components.PinColorHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed class DayFilter(open val label: String) {
    data object All : DayFilter("All Days")
    data object Today : DayFilter("Today")
    data class SpecificDate(val date: String, override val label: String) : DayFilter(label)
}

data class ConferenceUiState(
    val sessions: List<ConferenceSession> = emptyList(),
    val filteredSessions: List<ConferenceSession> = emptyList(),
    val locationGroups: List<LocationGroup> = emptyList(),
    val selectedLocationPin: LocationGroup? = null,
    val nextSession: ConferenceSession? = null,
    val selectedFilter: DayFilter = DayFilter.All,
    val availableFilters: List<DayFilter> = listOf(DayFilter.All, DayFilter.Today),
    val isLoading: Boolean = false,
    val locationSuggestions: List<LocationSuggestion> = emptyList(),
    val isSearchingLocations: Boolean = false
)

class ConferenceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ConferenceRepository
    private val locationSearchHelper: LocationSearchHelper

    private val _selectedFilter = MutableStateFlow<DayFilter>(DayFilter.All)
    val selectedFilter: StateFlow<DayFilter> = _selectedFilter.asStateFlow()

    private val _selectedLocationPin = MutableStateFlow<LocationGroup?>(null)
    val selectedLocationPin: StateFlow<LocationGroup?> = _selectedLocationPin.asStateFlow()

    private val _locationSuggestions = MutableStateFlow<List<LocationSuggestion>>(emptyList())
    val locationSuggestions: StateFlow<List<LocationSuggestion>> =
        _locationSuggestions.asStateFlow()

    private val _isSearchingLocations = MutableStateFlow(false)
    val isSearchingLocations: StateFlow<Boolean> = _isSearchingLocations.asStateFlow()

    private var locationSearchJob: Job? = null

    val uiState: StateFlow<ConferenceUiState>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ConferenceRepository(database.conferenceDao())
        locationSearchHelper = LocationSearchHelper(application)

        // App starts clean/empty on new installation
        viewModelScope.launch {
            repository.clearSampleEventsIfAny()
        }

        val coreUiState: StateFlow<ConferenceUiState> = combine(
            repository.allSessions,
            _selectedFilter,
            _selectedLocationPin
        ) { sessions, filter, selectedPin ->
            val todayStr = LocalDate.now().toString()
            val nowTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

            // Derive available filters based on unique dates from sessions
            val uniqueDates = sessions.map { it.date }.distinct().sorted()
            val availableFilters = mutableListOf<DayFilter>(DayFilter.All, DayFilter.Today)
            uniqueDates.forEach { date ->
                val friendlyDate = formatFriendlyDate(date)
                availableFilters.add(DayFilter.SpecificDate(date, friendlyDate))
            }

            // Filter sessions
            val filtered = when (filter) {
                is DayFilter.All -> sessions
                is DayFilter.Today -> sessions.filter { it.date == todayStr }
                is DayFilter.SpecificDate -> sessions.filter { it.date == filter.date }
            }

            // In the pins in the map, only show future events (ongoing or upcoming)
            val futureSessionsForMap = filtered.filter { session ->
                session.date > todayStr || (session.date == todayStr && session.endTime >= nowTime)
            }

            // Group only future sessions into location pins on the map
            val locationGroups = repository.groupSessionsByLocation(futureSessionsForMap)

            // Resolve next upcoming event (returns null if no more events)
            val nextSession = calculateNextEvent(sessions, todayStr)

            // If selected pin is no longer in filtered groups, reset or re-find
            val activePin = if (selectedPin != null) {
                locationGroups.firstOrNull { it.key == selectedPin.key }
            } else {
                null
            }

            ConferenceUiState(
                sessions = sessions,
                filteredSessions = filtered,
                locationGroups = locationGroups,
                selectedLocationPin = activePin,
                nextSession = nextSession,
                selectedFilter = filter,
                availableFilters = availableFilters,
                locationSuggestions = emptyList(),
                isSearchingLocations = false
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ConferenceUiState()
        )

        // Location suggestions are transient UI state. Keep them out of the expensive
        // session/map derivation so typing in the search box does not regroup every session.
        uiState = combine(
            coreUiState,
            _locationSuggestions,
            _isSearchingLocations
        ) { state, suggestions, isSearching ->
            state.copy(
                locationSuggestions = suggestions,
                isSearchingLocations = isSearching
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ConferenceUiState()
        )
    }

    fun setDayFilter(filter: DayFilter) {
        _selectedFilter.value = filter
    }

    fun selectLocationPin(pin: LocationGroup?) {
        _selectedLocationPin.value = pin
    }

    /**
     * Determines the color for a venue:
     * - If it's an already existing venue, reuses the exact same color.
     * - If it's a new venue, automatically picks an unused color from the palette,
     *   ensuring it NEVER reuses an existing color even if there are no future events happening there.
     */
    fun getOrAssignVenueColor(venueName: String, currentSessionId: Long = 0L): String {
        val cleanName = venueName.trim()
        if (cleanName.isBlank()) return PinColorHelper.PALETTE.first()

        val allSessions = uiState.value.sessions

        // 1. If an existing session at this venue already exists anywhere in the database,
        // use that same color so all sessions at this location share the identical pin color.
        val existingVenueSession = allSessions.firstOrNull {
            it.id != currentSessionId &&
                    it.locationName.trim().equals(cleanName, ignoreCase = true) &&
                    it.colorHex.isNotBlank()
        }
        if (existingVenueSession != null) {
            return existingVenueSession.colorHex
        }

        // If editing a session and venue didn't change, retain its current color
        if (currentSessionId != 0L) {
            val selfSession = allSessions.firstOrNull { it.id == currentSessionId }
            if (selfSession != null && selfSession.locationName.trim()
                    .equals(cleanName, ignoreCase = true) && selfSession.colorHex.isNotBlank()
            ) {
                return selfSession.colorHex
            }
        }

        // 2. New venue: collect ALL colors ever used across all sessions in the app.
        // Even if a venue has only past events (no future events happening there),
        // its color is in usedColors and MUST NOT be reused!
        val usedColors = allSessions
            .filter { it.id != currentSessionId && it.colorHex.isNotBlank() }
            .map { it.colorHex }
            .toSet()

        return PinColorHelper.getUnusedOrDistinctColor(usedColors, cleanName)
    }

    fun saveSession(session: ConferenceSession, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            // Automatically assign venue color: reuse existing venue color, or pick new unused color
            val finalColor = getOrAssignVenueColor(session.locationName, session.id)

            val sessionToSave = session.copy(colorHex = finalColor)
            if (sessionToSave.id == 0L) {
                repository.insertSession(sessionToSave)
            } else {
                repository.updateSession(sessionToSave)
            }
            onComplete()
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSessionById(sessionId)
            if (_selectedLocationPin.value?.sessions?.any { it.id == sessionId } == true) {
                _selectedLocationPin.value = null
            }
        }
    }

    fun exportSessionsJson(): String {
        return SessionJsonHelper.exportToJson(uiState.value.sessions)
    }

    fun importSessions(
        newSessions: List<ConferenceSession>,
        replaceAll: Boolean,
        onComplete: (Int) -> Unit = {}
    ) {
        viewModelScope.launch {
            if (replaceAll) {
                repository.clearAll()
            }
            val currentAll = if (replaceAll) emptyList() else uiState.value.sessions
            val processedSessions = mutableListOf<ConferenceSession>()
            val runningSessions = currentAll.toMutableList()

            for (rawSession in newSessions) {
                val cleanVenue = rawSession.locationName.trim()
                val assignedColor =
                    if (rawSession.colorHex.isNotBlank() && rawSession.colorHex.startsWith("#")) {
                        val existingColor = runningSessions.firstOrNull {
                            it.locationName.trim()
                                .equals(cleanVenue, ignoreCase = true) && it.colorHex.isNotBlank()
                        }?.colorHex
                        existingColor ?: rawSession.colorHex
                    } else {
                        val usedColors = runningSessions
                            .filter { it.colorHex.isNotBlank() }
                            .map { it.colorHex }
                            .toSet()
                        PinColorHelper.getUnusedOrDistinctColor(usedColors, cleanVenue)
                    }

                val finalized = rawSession.copy(id = 0L, colorHex = assignedColor)
                processedSessions.add(finalized)
                runningSessions.add(finalized)
            }

            repository.insertSessions(processedSessions)
            onComplete(processedSessions.size)
        }
    }

    fun resolveLocationSuggestion(placeId: String?, onResolved: (Pair<Double, Double>?) -> Unit) {
        if (placeId.isNullOrBlank()) {
            onResolved(null)
            return
        }

        viewModelScope.launch {
            onResolved(locationSearchHelper.resolvePlaceIdCoordinates(placeId))
        }
    }

    fun searchLocations(query: String) {
        locationSearchJob?.cancel()

        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) {
            _locationSuggestions.value = emptyList()
            _isSearchingLocations.value = false
            return
        }

        locationSearchJob = viewModelScope.launch {
            delay(300)
            _isSearchingLocations.value = true
            try {
                _locationSuggestions.value = locationSearchHelper.searchSuggestions(cleanQuery)
            } finally {
                _isSearchingLocations.value = false
            }
        }
    }

    private fun calculateNextEvent(
        sessions: List<ConferenceSession>,
        todayStr: String
    ): ConferenceSession? {
        if (sessions.isEmpty()) return null

        val nowTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

        // 1. Check today's upcoming or currently ongoing sessions
        val todayUpcoming = sessions
            .filter { it.date == todayStr && it.endTime >= nowTime }
            .sortedBy { it.startTime }

        if (todayUpcoming.isNotEmpty()) {
            return todayUpcoming.first()
        }

        // 2. Check future days
        val futureDays = sessions
            .filter { it.date > todayStr }
            .sortedWith(compareBy({ it.date }, { it.startTime }))

        if (futureDays.isNotEmpty()) {
            return futureDays.first()
        }

        // 3. No more events -> return null so no pill is shown
        return null
    }

    companion object {
        fun formatFriendlyDate(dateStr: String): String {
            return try {
                val parsed = LocalDate.parse(dateStr)
                parsed.format(DateTimeFormatter.ofPattern("d MMM", Locale.UK))
            } catch (_: Exception) {
                dateStr
            }
        }

        fun formatEuropeanDate(dateStr: String): String {
            return try {
                val parsed = LocalDate.parse(dateStr)
                parsed.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.UK))
            } catch (_: Exception) {
                dateStr
            }
        }
    }
}
