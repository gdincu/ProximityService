package ss.proximityservice.settings

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ss.proximityservice.ProximityService
import ss.proximityservice.R
import ss.proximityservice.data.Alert
import ss.proximityservice.data.AppStorage
import ss.proximityservice.data.Event
import ss.proximityservice.data.Mode
import ss.proximityservice.data.ServiceState

class SettingsViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private lateinit var mockAppStorage: AppStorage

    @Before
    fun setUp() {
        mockAppStorage = mock()
        // Sane defaults for ViewModel init.
        whenever(mockAppStorage.getInt(eq(OPERATIONAL_MODE), eq(Mode.DEFAULT.ordinal)))
            .thenReturn(Mode.DEFAULT.ordinal)
        whenever(mockAppStorage.getInt(eq(SCREEN_OFF_DELAY), eq(0))).thenReturn(0)
        whenever(mockAppStorage.getBoolean(eq(NOTIFICATION_DISMISS), eq(true))).thenReturn(true)
    }

    @After
    fun tearDown() {
        ProximityService.isRunning = false
        ServiceState.resetForTests()
    }

    @Test
    fun `serviceState with false initial value`() {
        ProximityService.isRunning = false
        ServiceState.resetForTests()

        val viewModel = SettingsViewModel(mock())

        assertThat(viewModel.serviceState.value).isFalse()
    }

    @Test
    fun `serviceState with true initial value`() {
        ProximityService.isRunning = true

        val viewModel = SettingsViewModel(mock())

        assertThat(viewModel.serviceState.value).isTrue()
    }

    @Test
    fun `updateState() to true results in observing true on serviceState`() {
        val viewModel = SettingsViewModel(mock())
        val observer: Observer<Boolean> = mock()
        viewModel.serviceState.observeForever(observer)
        reset(observer)

        viewModel.updateState(true)

        verify(observer).onChanged(eq(true))
    }

    @Test
    fun `updateState() to false results in observing false on serviceState`() {
        val viewModel = SettingsViewModel(mock())
        val observer: Observer<Boolean> = mock()
        viewModel.serviceState.observeForever(observer)
        reset(observer)

        viewModel.updateState(false)

        verify(observer).onChanged(eq(false))
    }

    @Test
    fun `getNextIntentAction() returns start action when service is not running`() {
        ProximityService.isRunning = false
        ServiceState.resetForTests()
        val viewModel = SettingsViewModel(mock())

        assertThat(viewModel.getNextIntentAction()).isEqualTo(ProximityService.INTENT_ACTION_START)
    }

    @Test
    fun `getNextIntentAction() returns stop action when service is running`() {
        ProximityService.isRunning = true
        val viewModel = SettingsViewModel(mock())

        assertThat(viewModel.getNextIntentAction()).isEqualTo(ProximityService.INTENT_ACTION_STOP)
    }

    @Test
    fun `notification behavior initial value respects stored dismiss true`() {
        whenever(mockAppStorage.getBoolean(eq(NOTIFICATION_DISMISS), eq(true))).thenReturn(true)

        val viewModel = SettingsViewModel(mockAppStorage)

        assertThat(viewModel.notificationBehaviorResId.value)
            .isEqualTo(R.string.settings_notification_behavior_secondary_dismiss)
    }

    @Test
    fun `notification behavior initial value respects stored dismiss false`() {
        whenever(mockAppStorage.getBoolean(eq(NOTIFICATION_DISMISS), eq(true))).thenReturn(false)

        val viewModel = SettingsViewModel(mockAppStorage)

        assertThat(viewModel.notificationBehaviorResId.value)
            .isEqualTo(R.string.settings_notification_behavior_secondary_retain)
    }

    @Test
    fun `screenOffDelayUpdate() should store the input progress Int`() {
        val viewModel = SettingsViewModel(mockAppStorage)

        viewModel.screenOffDelayUpdate(1)

        verify(mockAppStorage).put(eq(SCREEN_OFF_DELAY), eq(1))
    }

    @Test
    fun `screenOffDelayUpdate() clamps out-of-range values`() {
        val viewModel = SettingsViewModel(mockAppStorage)

        viewModel.screenOffDelayUpdate(99)

        verify(mockAppStorage).put(eq(SCREEN_OFF_DELAY), eq(6))
    }

    @Test
    fun `appStorage operational mode should be reset to default if stored value is not a known value`() {
        whenever(
            mockAppStorage.getInt(
                eq(OPERATIONAL_MODE),
                eq(Mode.DEFAULT.ordinal)
            )
        ).thenReturn(Int.MAX_VALUE)

        SettingsViewModel(mockAppStorage)

        verify(mockAppStorage).put(eq(OPERATIONAL_MODE), eq(Mode.DEFAULT.ordinal))
    }

    @Test
    fun `mockAppStorage screen off delay should be reset to zero if stored value is outside known range`() {
        whenever(mockAppStorage.getInt(eq(SCREEN_OFF_DELAY), eq(0))).thenReturn(Int.MAX_VALUE)

        SettingsViewModel(mockAppStorage)

        verify(mockAppStorage).put(eq(SCREEN_OFF_DELAY), eq(0))
    }

    @Test
    fun `observe Alert on operationalModeClick()`() {
        val viewModel = SettingsViewModel(mock())
        val observer: Observer<Event<Alert>> = mock()
        viewModel.alert.observeForever(observer)

        viewModel.operationalModeClick()

        val captor = argumentCaptor<Event<Alert>>()
        verify(observer).onChanged(captor.capture())
        assertThat(captor.firstValue).isNotNull()
    }

    @Test
    fun `observe Alert on notificationBehaviorClick()`() {
        val viewModel = SettingsViewModel(mock())
        val observer: Observer<Event<Alert>> = mock()
        viewModel.alert.observeForever(observer)

        viewModel.notificationBehaviorClick()

        val captor = argumentCaptor<Event<Alert>>()
        verify(observer).onChanged(captor.capture())
        assertThat(captor.firstValue).isNotNull()
    }
}
