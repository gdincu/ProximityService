package ss.proximityservice.settings

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ss.proximityservice.ProximityService
import ss.proximityservice.ProximityService.Companion.INTENT_ACTION_START
import ss.proximityservice.ProximityService.Companion.INTENT_ACTION_STOP
import ss.proximityservice.R
import ss.proximityservice.data.Alert
import ss.proximityservice.data.AppStorage
import ss.proximityservice.data.Event
import ss.proximityservice.data.Mode
import ss.proximityservice.data.ServiceState
import javax.inject.Inject

class SettingsViewModel @Inject constructor(private val appStorage: AppStorage) : ViewModel() {

    private val _serviceState = MutableLiveData<Boolean>()
    private val _alert = MutableLiveData<Event<Alert>>()
    private val _operationalModeResId = MutableLiveData<Int>()
    private val _notificationBehaviorResId = MutableLiveData<Int>()
    private val _screenOffDelayResId = MutableLiveData<Int>()
    private val _screenOffDelayProgress = MutableLiveData<Int>()

    val serviceState: LiveData<Boolean>
        get() = _serviceState

    val alert: LiveData<Event<Alert>>
        get() = _alert

    val operationalModeResId: LiveData<Int>
        get() = _operationalModeResId

    val notificationBehaviorResId: LiveData<Int>
        get() = _notificationBehaviorResId

    val screenOffDelayResId: LiveData<Int>
        get() = _screenOffDelayResId

    val screenOffDelayProgress: LiveData<Int>
        get() = _screenOffDelayProgress

    init {
        _serviceState.value = ServiceState.running || ProximityService.isRunning
        _operationalModeResId.value =
            when (appStorage.getInt(OPERATIONAL_MODE, Mode.DEFAULT.ordinal)) {
                Mode.DEFAULT.ordinal -> R.string.settings_operational_mode_secondary_default
                Mode.AMOLED_WAKELOCK.ordinal -> R.string.settings_operational_mode_secondary_amoled_wakelock
                Mode.AMOLED_NO_WAKELOCK.ordinal -> R.string.settings_operational_mode_secondary_amoled_no_wakelock
                else -> {
                    // reset to default
                    appStorage.put(OPERATIONAL_MODE, Mode.DEFAULT.ordinal)
                    R.string.settings_operational_mode_secondary_default
                }
            }
        _notificationBehaviorResId.value = if (appStorage.getBoolean(
                NOTIFICATION_DISMISS,
                true
            )
        ) R.string.settings_notification_behavior_secondary_dismiss else R.string.settings_notification_behavior_secondary_retain
        var screenOffDelay = appStorage.getInt(SCREEN_OFF_DELAY, 0)
        _screenOffDelayResId.value = when (screenOffDelay) {
            0 -> R.string.screen_off_delay_zero
            1 -> R.string.screen_off_delay_zero_half
            2 -> R.string.screen_off_delay_one
            3 -> R.string.screen_off_delay_one_half
            4 -> R.string.screen_off_delay_two
            5 -> R.string.screen_off_delay_two_half
            6 -> R.string.screen_off_delay_three
            else -> {
                // reset to zero
                appStorage.put(SCREEN_OFF_DELAY, 0)
                screenOffDelay = 0
                R.string.screen_off_delay_zero
            }
        }
        _screenOffDelayProgress.value = screenOffDelay
    }

    fun updateState(isRunning: Boolean) {
        _serviceState.postValue(isRunning)
    }

    fun getNextIntentAction(): String =
        if (ServiceState.running || ProximityService.isRunning) INTENT_ACTION_STOP else INTENT_ACTION_START

    fun operationalModeClick() {
        _alert.postValue(Event(object : Alert {
            override fun show(context: Context) {
                MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_App_MaterialAlertDialog)
                    .setTitle(R.string.settings_operational_mode_title)
                    .setMessage(R.string.settings_operational_mode_description)
                    .setPositiveButton(R.string.settings_operational_mode_secondary_default) { _, _ ->
                        appStorage.put(OPERATIONAL_MODE, Mode.DEFAULT.ordinal)
                        _operationalModeResId.postValue(R.string.settings_operational_mode_secondary_default)
                    }
                    .setNegativeButton(R.string.settings_operational_mode_secondary_amoled_wakelock) { _, _ ->
                        appStorage.put(OPERATIONAL_MODE, Mode.AMOLED_WAKELOCK.ordinal)
                        _operationalModeResId.postValue(R.string.settings_operational_mode_secondary_amoled_wakelock)
                    }
                    .setNeutralButton(R.string.settings_operational_mode_secondary_amoled_no_wakelock) { _, _ ->
                        appStorage.put(OPERATIONAL_MODE, Mode.AMOLED_NO_WAKELOCK.ordinal)
                        _operationalModeResId.postValue(R.string.settings_operational_mode_secondary_amoled_no_wakelock)
                    }
                    .show()
            }
        }))
    }

    fun notificationBehaviorClick() {
        _alert.postValue(Event(object : Alert {
            override fun show(context: Context) {
                MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_App_MaterialAlertDialog)
                    .setTitle(R.string.settings_notification_behavior_title)
                    .setMessage(R.string.settings_notification_behavior_description)
                    .setPositiveButton(R.string.dismiss) { _, _ ->
                        appStorage.put(NOTIFICATION_DISMISS, true)
                        _notificationBehaviorResId.postValue(R.string.settings_notification_behavior_secondary_dismiss)
                    }
                    .setNegativeButton(R.string.retain) { _, _ ->
                        appStorage.put(NOTIFICATION_DISMISS, false)
                        _notificationBehaviorResId.postValue(R.string.settings_notification_behavior_secondary_retain)
                    }
                    .show()
            }
        }))
    }

    fun screenOffDelayProgress(progress: Int) {
        _screenOffDelayResId.postValue(delayResId(progress) ?: return)
    }

    fun screenOffDelayUpdate(progress: Int) {
        val clamped = progress.coerceIn(0, 6)
        appStorage.put(SCREEN_OFF_DELAY, clamped)
    }

    @StringRes
    private fun delayResId(progress: Int): Int? = when (progress) {
        0 -> R.string.screen_off_delay_zero
        1 -> R.string.screen_off_delay_zero_half
        2 -> R.string.screen_off_delay_one
        3 -> R.string.screen_off_delay_one_half
        4 -> R.string.screen_off_delay_two
        5 -> R.string.screen_off_delay_two_half
        6 -> R.string.screen_off_delay_three
        else -> null
    }
}
