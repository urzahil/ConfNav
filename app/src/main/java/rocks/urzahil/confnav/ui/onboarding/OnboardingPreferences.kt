package rocks.urzahil.confnav.ui.onboarding

import android.content.Context

// Remembers whether the welcome slides have been shown, so they only appear on first launch.
object OnboardingPreferences {
    private const val PREFS_NAME = "onboarding"
    private const val KEY_COMPLETED = "completed"

    fun isCompleted(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_COMPLETED, false)

    fun markCompleted(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_COMPLETED, true)
            .apply()
    }
}
