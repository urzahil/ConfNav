package rocks.urzahil.confnav.shared

import com.google.android.gms.wearable.DataMap

/**
 * The Wearable Data Layer contract between the phone and the watch. The phone publishes the
 * whole agenda as one data item at [PATH]; the watch reads its local copy of that item, so the
 * tile and complication keep working while the phone is out of range.
 */
object AgendaSync {

    const val PATH = "/confnav/agenda"

    private const val KEY_SESSIONS = "sessions"
    private const val KEY_TITLE = "title"
    private const val KEY_SPEAKER = "speaker"
    private const val KEY_DATE = "date"
    private const val KEY_START = "startTime"
    private const val KEY_END = "endTime"
    private const val KEY_LOCATION = "locationName"
    private const val KEY_LATITUDE = "latitude"
    private const val KEY_LONGITUDE = "longitude"
    private const val KEY_COLOR = "colorHex"
    private const val KEY_STARRED = "starred"

    fun toDataMap(sessions: List<WatchSession>): DataMap = DataMap().apply {
        putDataMapArrayList(KEY_SESSIONS, ArrayList(sessions.map { it.toDataMap() }))
    }

    fun fromDataMap(map: DataMap): List<WatchSession> =
        map.getDataMapArrayList(KEY_SESSIONS).orEmpty().map { it.toWatchSession() }

    private fun WatchSession.toDataMap(): DataMap = DataMap().apply {
        putString(KEY_TITLE, title)
        putString(KEY_SPEAKER, speaker)
        putString(KEY_DATE, date)
        putString(KEY_START, startTime)
        putString(KEY_END, endTime)
        putString(KEY_LOCATION, locationName)
        putDouble(KEY_LATITUDE, latitude)
        putDouble(KEY_LONGITUDE, longitude)
        putString(KEY_COLOR, colorHex)
        putBoolean(KEY_STARRED, starred)
    }

    private fun DataMap.toWatchSession(): WatchSession = WatchSession(
        title = getString(KEY_TITLE, ""),
        speaker = getString(KEY_SPEAKER, ""),
        date = getString(KEY_DATE, ""),
        startTime = getString(KEY_START, ""),
        endTime = getString(KEY_END, ""),
        locationName = getString(KEY_LOCATION, ""),
        latitude = getDouble(KEY_LATITUDE, 0.0),
        longitude = getDouble(KEY_LONGITUDE, 0.0),
        colorHex = getString(KEY_COLOR, ""),
        starred = getBoolean(KEY_STARRED, false)
    )
}
