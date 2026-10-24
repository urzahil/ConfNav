package rocks.urzahil.confnav.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import rocks.urzahil.confnav.data.local.AppDatabase
import rocks.urzahil.confnav.data.model.ConferenceSession
import rocks.urzahil.confnav.data.model.ImportReport
import rocks.urzahil.confnav.data.model.LocationGroup
import rocks.urzahil.confnav.data.model.SessionJsonHelper
import rocks.urzahil.confnav.data.network.LocationSearchHelper
import rocks.urzahil.confnav.data.network.LocationSuggestion
import rocks.urzahil.confnav.data.repository.ConferenceRepository
import rocks.urzahil.confnav.ui.components.PinColorHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

sealed class DayFilter(open val label: String) {
    data object All : DayFilter("All Days")
    data class SpecificDate(val date: String, override val label: String) : DayFilter(label)
}

data class ConferenceUiState(
    val sessions: List<ConferenceSession> = emptyList(),
    val filteredSessions: List<ConferenceSession> = emptyList(),
    val locationGroups: List<LocationGroup> = emptyList(),
    val selectedLocationPin: LocationGroup? = null,
    val nextSession: ConferenceSession? = null,
    val selectedFilter: DayFilter = DayFilter.All,
    val availableFilters: List<DayFilter> = listOf(DayFilter.All),
    val isLoading: Boolean = false,
    val locationSuggestions: List<LocationSuggestion> = emptyList(),
    val isSearchingLocations: Boolean = false
)

/** What an import did: sessions written, and sessions skipped because the agenda already had them. */
data class ImportOutcome(val added: Int, val duplicates: Int)

private data class LocationSearchState(
    val suggestions: List<LocationSuggestion> = emptyList(),
    val isSearching: Boolean = false
)

class ConferenceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ConferenceRepository
    private val locationSearchHelper: LocationSearchHelper

    private val selectedFilter = MutableStateFlow<DayFilter>(DayFilter.All)
    private val selectedLocationPin = MutableStateFlow<LocationGroup?>(null)
    private val locationSearch = MutableStateFlow(LocationSearchState())

    private var locationSearchJob: Job? = null

    val uiState: StateFlow<ConferenceUiState>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ConferenceRepository(database.conferenceDao())
        locationSearchHelper = LocationSearchHelper(application)

        uiState = combine(
            repository.allSessions,
            selectedFilter,
            selectedLocationPin,
            minuteTicker(),
            locationSearch
        ) { sessions, filter, selectedPin, now, search ->
            buildState(sessions, filter, selectedPin, now, search)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ConferenceUiState()
        )
    }

    /** Emits the current time now and again at the start of every minute while collected. */
    private fun minuteTicker(): Flow<LocalDateTime> = flow {
        while (true) {
            val now = LocalDateTime.now()
            emit(now)
            val intoMinuteMs = now.second * 1000L + now.nano / 1_000_000L
            delay((60_000L - intoMinuteMs).coerceAtLeast(1_000L))
        }
    }

    private fun buildState(
        sessions: List<ConferenceSession>,
        filter: DayFilter,
        selectedPin: LocationGroup?,
        now: LocalDateTime,
        search: LocationSearchState
    ): ConferenceUiState {
        val availableFilters = mutableListOf<DayFilter>(DayFilter.All)
        sessions.map { it.date }.distinct().sorted().forEach { date ->
            availableFilters.add(DayFilter.SpecificDate(date, formatFriendlyDate(date)))
        }

        val filtered = when (filter) {
            is DayFilter.All -> sessions
            is DayFilter.SpecificDate -> sessions.filter { it.date == filter.date }
        }

        // The map only shows sessions that are ongoing or still to come.
        val locationGroups = repository.groupSessionsByLocation(
            filtered.filter { AgendaLogic.isOngoingOrUpcoming(it, now) }
        )

        // If the selected pin is no longer present, drop the selection.
        val activePin = selectedPin?.let { pin -> locationGroups.firstOrNull { it.key == pin.key } }

        return ConferenceUiState(
            sessions = sessions,
            filteredSessions = filtered,
            locationGroups = locationGroups,
            selectedLocationPin = activePin,
            nextSession = AgendaLogic.nextSession(sessions, now),
            selectedFilter = filter,
            availableFilters = availableFilters,
            locationSuggestions = search.suggestions,
            isSearchingLocations = search.isSearching
        )
    }

    fun setDayFilter(filter: DayFilter) {
        selectedFilter.value = filter
    }

    fun selectLocationPin(pin: LocationGroup?) {
        selectedLocationPin.value = pin
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
            if (selectedLocationPin.value?.sessions?.any { it.id == sessionId } == true) {
                selectedLocationPin.value = null
            }
        }
    }

    fun exportSessionsJson(): String {
        return SessionJsonHelper.exportToJson(uiState.value.sessions)
    }

    /** Parses file contents; the caller decides what to do with the report. */
    fun parseImport(jsonText: String): Result<ImportReport> = SessionJsonHelper.parse(jsonText)

    fun importSessions(
        newSessions: List<ConferenceSession>,
        replaceAll: Boolean,
        onComplete: (ImportOutcome) -> Unit = {}
    ) {
        viewModelScope.launch {
            // Read the database directly so the result never depends on a stale UI snapshot.
            val existing = if (replaceAll) emptyList() else repository.allSessions.first()
            val knownKeys = existing.map { AgendaLogic.duplicateKey(it) }.toHashSet()
            val toAdd = newSessions.filter { knownKeys.add(AgendaLogic.duplicateKey(it)) }
            val duplicates = newSessions.size - toAdd.size

            val processedSessions = mutableListOf<ConferenceSession>()
            val runningSessions = existing.toMutableList()

            for (rawSession in toAdd) {
                val cleanVenue = rawSession.locationName.trim()
                // Sessions at the same venue always share one pin colour.
                val venueColor = runningSessions.firstOrNull {
                    it.locationName.trim().equals(cleanVenue, ignoreCase = true) &&
                            it.colorHex.isNotBlank()
                }?.colorHex
                val assignedColor = venueColor
                    ?: rawSession.colorHex.takeIf { it.isNotBlank() }
                    ?: PinColorHelper.getUnusedOrDistinctColor(
                        runningSessions.filter { it.colorHex.isNotBlank() }.map { it.colorHex }.toSet(),
                        cleanVenue
                    )

                val finalized = rawSession.copy(id = 0L, colorHex = assignedColor)
                processedSessions.add(finalized)
                runningSessions.add(finalized)
            }

            if (replaceAll) {
                repository.replaceAll(processedSessions)
            } else if (processedSessions.isNotEmpty()) {
                repository.insertSessions(processedSessions)
            }
            onComplete(ImportOutcome(added = processedSessions.size, duplicates = duplicates))
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
            locationSearch.value = LocationSearchState()
            return
        }

        locationSearchJob = viewModelScope.launch {
            delay(300)
            locationSearch.value = locationSearch.value.copy(isSearching = true)
            try {
                val results = locationSearchHelper.searchSuggestions(cleanQuery)
                locationSearch.value = LocationSearchState(suggestions = results, isSearching = true)
            } finally {
                locationSearch.value = locationSearch.value.copy(isSearching = false)
            }
        }
    }

    companion object {
        /** Short day label for filter chips, in the device's language (e.g. "16 Oct"). */
        fun formatFriendlyDate(dateStr: String): String {
            return try {
                LocalDate.parse(dateStr)
                    .format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
            } catch (_: Exception) {
                dateStr
            }
        }

        /** Date in the device's locale and preferred style (e.g. "Oct 16, 2026"). */
        fun formatShortDate(dateStr: String): String {
            return try {
                LocalDate.parse(dateStr)
                    .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()))
            } catch (_: Exception) {
                dateStr
            }
        }
    }
}
