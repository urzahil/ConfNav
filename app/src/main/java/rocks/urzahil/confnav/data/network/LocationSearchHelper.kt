package rocks.urzahil.confnav.data.network

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import rocks.urzahil.confnav.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

class LocationSearchHelper(
    private val context: Context,
    private val service: GoogleGeocodingService = GoogleGeocodingService.create()
) {
    companion object {
        private const val TAG = "LocationSearchHelper"

        private val STARTS_WITH_DIGITS = Regex("^[0-9]+.*")
        private val DIGITS_ONLY = Regex("^[0-9]+$")
        private val HOUSE_NUMBER_ONLY = Regex("^[0-9]+[A-Za-z]?$")
        /** "12 Main St", "12A Main St", "12, Main St": a house number followed by the street. */
        private val HOUSE_NUMBER_THEN_STREET = Regex("^[0-9]+[A-Za-z]?,? .*")
    }

    /** Empty when no real key was configured (the `.env.example` placeholder counts as none). */
    private val apiKey: String = BuildConfig.MAPS_API_KEY
        .takeIf { it.isNotBlank() && !it.contains("YOUR_GOOGLE_MAPS_API_KEY") }
        .orEmpty()

    suspend fun searchSuggestions(query: String): List<LocationSuggestion> =
        withContext(Dispatchers.IO) {
            val cleanQuery = query.trim()
            if (cleanQuery.isEmpty()) {
                return@withContext emptyList()
            }

            val results = mutableListOf<LocationSuggestion>()

            // 1. Google Places Autocomplete API
            if (apiKey.isNotEmpty()) {
                try {
                    val response = service.getPlaceAutocomplete(input = cleanQuery, apiKey = apiKey)
                    if (response.status == "OK" && response.predictions.isNotEmpty()) {
                        for (pred in response.predictions) {
                            val mainText = pred.structuredFormatting?.mainText
                            val secondaryText = pred.structuredFormatting?.secondaryText ?: ""
                            val fullAddress = pred.description.orEmpty()

                            // Determine descriptive venue/location name
                            val isMainTextPureStreetAddress = !mainText.isNullOrBlank() &&
                                    mainText.matches(HOUSE_NUMBER_THEN_STREET)

                            val descriptiveName = when {
                                !mainText.isNullOrBlank() && !isMainTextPureStreetAddress -> mainText.trim()
                                !cleanQuery.matches(STARTS_WITH_DIGITS) -> {
                                    cleanQuery.split(",").first().trim().replaceFirstChar {
                                        if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
                                    }
                                }

                                !mainText.isNullOrBlank() -> mainText.trim()
                                else -> fullAddress.split(",").firstOrNull()?.trim() ?: cleanQuery
                            }

                            // Coordinates are resolved from the place_id only once the user picks it.
                            results.add(
                                LocationSuggestion(
                                    name = descriptiveName,
                                    address = if (secondaryText.isNotBlank()) secondaryText else fullAddress,
                                    latitude = 0.0,
                                    longitude = 0.0,
                                    placeId = pred.placeId
                                )
                            )
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Google Places autocomplete failed", e)
                }
            }

            // 2. System Geocoder fallback if Google Places returned no results (e.g. offline/no key)
            if (results.isEmpty()) {
                try {
                    val geocoded = geocodeWithSystemGeocoder(cleanQuery)
                    for (addr in geocoded) {
                        val fullAddress =
                            (0..addr.maxAddressLineIndex).mapNotNull { addr.getAddressLine(it) }
                                .joinToString(", ")
                        val descriptiveName = resolveDescriptiveName(
                            premises = addr.premises,
                            featureName = addr.featureName,
                            thoroughfare = addr.thoroughfare,
                            query = cleanQuery,
                            fullAddress = fullAddress
                        )

                        if (results.none { it.latitude == addr.latitude && it.longitude == addr.longitude }) {
                            results.add(
                                LocationSuggestion(
                                    name = descriptiveName,
                                    address = if (fullAddress.isNotBlank()) fullAddress else descriptiveName,
                                    latitude = addr.latitude,
                                    longitude = addr.longitude
                                )
                            )
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "System Geocoder failed", e)
                }
            }

            results
        }

    private fun resolveDescriptiveName(
        premises: String?,
        featureName: String?,
        thoroughfare: String?,
        query: String,
        fullAddress: String
    ): String {
        // 1. If premises is a named venue (e.g. "Moscone Center South", "Building 4")
        if (!premises.isNullOrBlank() && !premises.matches(STARTS_WITH_DIGITS)) {
            return premises.trim()
        }

        // 2. If featureName is a named venue/building/POI
        if (!featureName.isNullOrBlank()) {
            val trimmed = featureName.trim()
            val isJustNumber = trimmed.matches(HOUSE_NUMBER_ONLY)
            val isStreetName =
                thoroughfare != null && trimmed.equals(thoroughfare.trim(), ignoreCase = true)
            // If it's not just a house number and not just the street name, it's a POI / venue name!
            if (!isJustNumber && !isStreetName) {
                return trimmed
            }
        }

        // 3. If user searched for a descriptive place (e.g. "Moscone Center", "Main Auditorium", "Ballroom A"),
        // use their descriptive search term as the venue name so it doesn't get replaced with raw house numbers/street address
        val clean = query.trim()
        if (clean.isNotBlank() && !clean.matches(HOUSE_NUMBER_THEN_STREET) && !clean.matches(DIGITS_ONLY)) {
            return clean.split(",").first().trim()
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
        }

        // 4. If fullAddress starts with a place name before commas
        val firstSegment = fullAddress.split(",").firstOrNull()?.trim().orEmpty()
        if (firstSegment.isNotBlank() && !firstSegment.matches(STARTS_WITH_DIGITS)) {
            return firstSegment
        }

        return fullAddress.split(",").firstOrNull()?.trim() ?: "Conference Venue"
    }

    suspend fun resolvePlaceIdCoordinates(placeId: String): Pair<Double, Double>? =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) return@withContext null

            try {
                val response = service.geocodePlaceId(placeId = placeId, apiKey = apiKey)
                val location = response.results.firstOrNull()?.geometry?.location
                if (location != null) {
                    return@withContext Pair(location.lat, location.lng)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Error resolving selected place coordinates", e)
            }

            null
        }

    private suspend fun geocodeWithSystemGeocoder(query: String): List<Address> =
        withContext(Dispatchers.IO) {
            if (!Geocoder.isPresent()) return@withContext emptyList()
            val geocoder = Geocoder(context, Locale.getDefault())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine<List<Address>> { cont ->
                    geocoder.getFromLocationName(query, 5, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            cont.resume(addresses)
                        }

                        override fun onError(errorMessage: String?) {
                            cont.resume(emptyList())
                        }
                    })
                }
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocationName(query, 5) ?: emptyList()
            }
        }
}
