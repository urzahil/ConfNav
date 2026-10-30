package rocks.urzahil.confnav.wear

import android.content.ComponentName
import android.util.Log
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService

/** Woken by the Data Layer when the phone sends a new agenda; redraws the tile and complication. */
class AgendaListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.i(TAG, "Agenda data changed (${dataEvents.count} event(s))")
        TileService.getUpdater(this).requestUpdate(NextEventTileService::class.java)
        ComplicationDataSourceUpdateRequester
            .create(this, ComponentName(this, NextEventComplicationService::class.java))
            .requestUpdateAll()
    }

    private companion object {
        const val TAG = "AgendaListener"
    }
}
