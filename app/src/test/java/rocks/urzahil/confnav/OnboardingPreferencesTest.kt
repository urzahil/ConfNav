package rocks.urzahil.confnav

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import rocks.urzahil.confnav.ui.onboarding.OnboardingPreferences

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OnboardingPreferencesTest {

    @Test
    fun `onboarding is shown until it is marked completed`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertFalse(OnboardingPreferences.isCompleted(context))

        OnboardingPreferences.markCompleted(context)

        assertTrue(OnboardingPreferences.isCompleted(context))
    }
}
