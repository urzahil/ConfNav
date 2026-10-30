package rocks.urzahil.confnav.wear

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.wear.remote.interactions.RemoteActivityHelper
import rocks.urzahil.confnav.shared.WatchSession

/**
 * Opened by the tile's Directions button and the complication. Starts walking navigation in
 * Google Maps on the watch; on a watch without Maps, opens the directions on the phone instead.
 * It has no UI of its own and finishes straight away.
 */
class DirectionsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val latitude = intent.getDoubleExtra(EXTRA_LATITUDE, Double.NaN)
        val longitude = intent.getDoubleExtra(EXTRA_LONGITUDE, Double.NaN)
        if (!latitude.isNaN() && !longitude.isNaN() && !openOnWatch(latitude, longitude)) {
            openOnPhone(latitude, longitude, intent.getStringExtra(EXTRA_NAME).orEmpty())
        }
        finish()
    }

    private fun openOnWatch(latitude: Double, longitude: Double): Boolean {
        val navigation = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$latitude,$longitude&mode=w"))
            .setPackage(MAPS_PACKAGE)
        return try {
            startActivity(navigation)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    private fun openOnPhone(latitude: Double, longitude: Double, name: String) {
        val directions = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude&travelmode=walking")
        ).addCategory(Intent.CATEGORY_BROWSABLE)

        val app = applicationContext
        val started = RemoteActivityHelper(app).startRemoteActivity(directions)
        started.addListener({
            try {
                started.get()
            } catch (e: Exception) {
                // For example a RemoteIntentException when the phone is out of range.
                Log.w(TAG, "Could not open directions on the phone", e)
            }
        }, app.mainExecutor)
        val label = name.ifBlank { "your session" }
        Toast.makeText(app, "Opening directions to $label on your phone", Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_LATITUDE = "rocks.urzahil.confnav.wear.LATITUDE"
        const val EXTRA_LONGITUDE = "rocks.urzahil.confnav.wear.LONGITUDE"
        const val EXTRA_NAME = "rocks.urzahil.confnav.wear.NAME"

        private const val TAG = "DirectionsActivity"
        private const val MAPS_PACKAGE = "com.google.android.apps.maps"

        fun intent(context: Context, session: WatchSession): Intent =
            Intent(context, DirectionsActivity::class.java)
                .putExtra(EXTRA_LATITUDE, session.latitude)
                .putExtra(EXTRA_LONGITUDE, session.longitude)
                .putExtra(EXTRA_NAME, session.locationName)
    }
}
