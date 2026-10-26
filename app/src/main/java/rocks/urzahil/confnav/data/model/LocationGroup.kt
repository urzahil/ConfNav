package rocks.urzahil.confnav.data.model

data class LocationGroup(
    val key: String,
    val locationName: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val colorHex: String,
    val sessions: List<ConferenceSession>
) {
    val sessionCount: Int get() = sessions.size
}
