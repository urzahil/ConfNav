package rocks.urzahil.confnav.wear

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import rocks.urzahil.confnav.shared.AgendaSync
import rocks.urzahil.confnav.shared.WatchSession

/** The agenda as last synced from the phone, or null if the phone has never sent one. */
object AgendaStore {

    private const val TAG = "AgendaStore"

    /**
     * Reads the watch's local copy of the agenda data item, which the Data Layer keeps on the
     * watch, so this works while the phone is out of range.
     */
    suspend fun load(context: Context): List<WatchSession>? {
        val buffer = try {
            Wearable.getDataClient(context).dataItems.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Could not read the synced agenda", e)
            return null
        }
        return try {
            val item = buffer.firstOrNull { it.uri.path == AgendaSync.PATH }
            Log.i(TAG, "Data layer holds ${buffer.count} item(s), agenda item found: ${item != null}")
            item?.let { AgendaSync.fromDataMap(DataMapItem.fromDataItem(it).dataMap) }
                ?.also { Log.i(TAG, "Decoded ${it.size} session(s) from the synced agenda") }
        } finally {
            buffer.release()
        }
    }
}
