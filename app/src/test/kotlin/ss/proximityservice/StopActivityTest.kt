package ss.proximityservice

import android.os.Build
import androidx.test.ext.junit.rules.activityScenarioRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(RobolectricTestRunner::class)
class StopActivityTest {

    @get:Rule
    val scenarioRule = activityScenarioRule<StopActivity>()

    @Test
    fun `stops ProximityService`() {
        scenarioRule.scenario.onActivity { activity ->
            val serviceIntent = nextStartedServiceIntent(activity)
            assertThat(serviceIntent?.action).isEqualTo(ProximityService.INTENT_ACTION_STOP)
            assertThat(serviceIntent?.component?.className).isEqualTo(ProximityService::class.java.canonicalName)
        }
    }

    @Test
    fun `is finishing immediately`() {
        scenarioRule.scenario.onActivity { activity ->
            assertThat(activity.isFinishing).isTrue()
        }
    }
}
