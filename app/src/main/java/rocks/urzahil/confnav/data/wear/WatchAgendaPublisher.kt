package rocks.urzahil.confnav.data.wear

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import rocks.urzahil.confnav.data.model.ConferenceSession
import rocks.urzahil.confnav.shared.AgendaSync
import rocks.urzahil.confnav.shared.WatchSession
import rocks.urzahil.confnav.ui.AgendaLogic
import java.time.LocalDateTime

/**
 * Sends the agenda to the ConfNav Wear OS app over the Wearable Data Layer. The data item is
 * kept on both devices and synced over Bluetooth, so the watch has it even while offline.
 */
class WatchAgendaPublisher(context: Context) {

    private val appContext = context.applicationContext

    suspend fun publish(sessions: List<ConferenceSession>, now: LocalDateTime = LocalDateTime.now()) {
        // Sessions that are over are never shown on the watch, and leaving them out keeps the
        // data item well under the Data Layer's 100 KB limit.
        val upcoming = sessions
            .filter { AgendaLogic.isOngoingOrUpcoming(it, now) }
            .sortedBy { AgendaLogic.startOf(it) }
            .take(MAX_SESSIONS)
            .map { it.toWatchSession() }

        val request = PutDataMapRequest.create(AgendaSync.PATH).apply {
            dataMap.putAll(AgendaSync.toDataMap(upcoming))
        }.asPutDataRequest().setUrgent()

        try {
            Wearable.getDataClient(appContext).putDataItem(request).await()
            Log.i(TAG, "Sent ${upcoming.size} of ${sessions.size} session(s) to the watch")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Phones without Wear OS support or Play services have no watch to sync to.
            Log.d(TAG, "Agenda not sent to the watch", e)
        }
    }

    private fun ConferenceSession.toWatchSession() = WatchSession(
        title = title,
        speaker = speaker,
        date = date,
        startTime = startTime,
        endTime = endTime,
        locationName = locationName,
        latitude = latitude,
        longitude = longitude,
        colorHex = colorHex,
        starred = starred
    )

    private companion object {
        const val TAG = "WatchAgendaPublisher"
        const val MAX_SESSIONS = 250
    }
}
