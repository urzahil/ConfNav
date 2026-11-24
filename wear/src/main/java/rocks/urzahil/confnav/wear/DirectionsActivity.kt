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
        // The activity is exported, so any app can start it: only act on real coordinates.
        if (isUsableCoordinate(latitude, longitude)) {
            if (!openOnWatch(latitude, longitude)) {
                openOnPhone(latitude, longitude, intent.getStringExtra(EXTRA_NAME).orEmpty())
            }
        } else {
            Log.w(TAG, "Ignoring directions request with invalid coordinates")
            Toast.makeText(applicationContext, "No location set for this session", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    /** Same rule as `WatchSession.hasLocation`: finite, in range, and not the 0,0 "not set" value. */
    private fun isUsableCoordinate(latitude: Double, longitude: Double): Boolean =
        latitude.isFinite() && longitude.isFinite() &&
                latitude in -90.0..90.0 && longitude in -180.0..180.0 &&
                !(latitude == 0.0 && longitude == 0.0)

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
        val label = name.ifBlank { "your session" }
        val started = RemoteActivityHelper(app).startRemoteActivity(directions)
        started.addListener({
            try {
                started.get()
                Toast.makeText(app, "Opening directions to $label on your phone", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                // For example a RemoteIntentException when the phone is out of range.
                Log.w(TAG, "Could not open directions on the phone", e)
                Toast.makeText(app, "Couldn't reach your phone", Toast.LENGTH_SHORT).show()
            }
        }, app.mainExecutor)
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
