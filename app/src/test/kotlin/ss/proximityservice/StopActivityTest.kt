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

@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(RobolectricTestRunner::class)
class StopActivityTest {

    @Test
    fun `stops ProximityService`() {
        val controller = Robolectric.buildActivity(StopActivity::class.java).setup()
        val serviceIntent = nextStartedServiceIntent()
        assertThat(serviceIntent?.action).isEqualTo(ProximityService.INTENT_ACTION_STOP)
        assertThat(serviceIntent?.component?.className).isEqualTo(ProximityService::class.java.name)
        controller.destroy()
    }

    @Test
    fun `is finishing immediately`() {
        val controller = Robolectric.buildActivity(StopActivity::class.java).setup()
        val activity = controller.get()
        assertThat(activity.isFinishing).isTrue()
        controller.destroy()
    }

    private fun nextStartedServiceIntent(): Intent? {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val shadowApp = shadowOf(app)
        // startForegroundService() is also recorded as a started service by Robolectric.
        runCatching<Intent?> { shadowApp.nextStartedService }.getOrNull()?.let { return it }
        runCatching<Intent?> { shadowApp.peekNextStartedService() }.getOrNull()?.let { return it }
        return null
    }
}
