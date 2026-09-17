package ss.proximityservice

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import ss.proximityservice.ProximityService.Companion.INTENT_ACTION_START

@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(RobolectricTestRunner::class)
class StartActivityTest {

    @Test
    fun `starts ProximityService`() {
        val controller = Robolectric.buildActivity(StartActivity::class.java).setup()
        val serviceIntent = nextStartedServiceIntent()
        assertThat(serviceIntent?.action).isEqualTo(INTENT_ACTION_START)
        assertThat(serviceIntent?.component?.className).isEqualTo(ProximityService::class.java.name)
        controller.destroy()
    }

    @Test
    fun `is finishing immediately`() {
        val controller = Robolectric.buildActivity(StartActivity::class.java).setup()
        val activity = controller.get()
        assertThat(activity.isFinishing).isTrue()
        controller.destroy()
    }

    private fun nextStartedServiceIntent(): Intent? {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val shadowApp = shadowOf(app)
        runCatching { shadowApp.nextStartedService }.getOrNull()?.let { return it }
        runCatching { shadowApp.peekNextStartedService() }.getOrNull()?.let { return it }
        runCatching { shadowApp.nextStartedForegroundService }.getOrNull()?.let { return it }
        runCatching { shadowApp.peekNextStartedForegroundService() }.getOrNull()?.let { return it }
        return null
    }
}
