package com.example.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PlacesAutocompleteResponse(
    @Json(name = "status") val status: String? = null,
    @Json(name = "predictions") val predictions: List<Prediction> = emptyList(),
    @Json(name = "error_message") val errorMessage: String? = null
)

@JsonClass(generateAdapter = true)
data class Prediction(
    @Json(name = "description") val description: String? = null,
    @Json(name = "place_id") val placeId: String? = null,
    @Json(name = "structured_formatting") val structuredFormatting: StructuredFormatting? = null
)

@JsonClass(generateAdapter = true)
data class StructuredFormatting(
    @Json(name = "main_text") val mainText: String? = null,
    @Json(name = "secondary_text") val secondaryText: String? = null
)

@JsonClass(generateAdapter = true)
data class GeocodeResponse(
    @Json(name = "status") val status: String? = null,
    @Json(name = "results") val results: List<GeocodeResult> = emptyList(),
    @Json(name = "error_message") val errorMessage: String? = null
)

@JsonClass(generateAdapter = true)
data class GeocodeResult(
    @Json(name = "formatted_address") val formattedAddress: String? = null,
    @Json(name = "place_id") val placeId: String? = null,
    @Json(name = "geometry") val geometry: Geometry? = null
)

@JsonClass(generateAdapter = true)
data class Geometry(
    @Json(name = "location") val location: LatLngLiteral? = null
)

@JsonClass(generateAdapter = true)
data class LatLngLiteral(
    @Json(name = "lat") val lat: Double = 0.0,
    @Json(name = "lng") val lng: Double = 0.0
)

data class LocationSuggestion(
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val placeId: String? = null,
    val isPreset: Boolean = false
)
