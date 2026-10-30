package rocks.urzahil.confnav.shared

/**
 * The part of a conference session the watch needs: what it is, when, where, and its pin colour.
 * Dates and times use the same `YYYY-MM-DD` / `HH:mm` strings as the phone's agenda.
 */
data class WatchSession(
    val title: String,
    val speaker: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val colorHex: String,
    val starred: Boolean = false
) {
    /** Same rule as the phone app: 0,0 means "not set", so such sessions have no directions. */
    val hasLocation: Boolean
        get() = latitude.isFinite() && longitude.isFinite() &&
                latitude in -90.0..90.0 && longitude in -180.0..180.0 &&
                !(latitude == 0.0 && longitude == 0.0)
}
