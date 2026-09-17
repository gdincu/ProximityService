package ss.proximityservice

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import dagger.android.DaggerService
import ss.proximityservice.data.AppStorage
import ss.proximityservice.data.Mode
import ss.proximityservice.data.ProximityDetector
import ss.proximityservice.data.ServiceState
import ss.proximityservice.settings.NOTIFICATION_DISMISS
import ss.proximityservice.settings.OPERATIONAL_MODE
import ss.proximityservice.settings.SCREEN_OFF_DELAY
import ss.proximityservice.testing.OpenForTesting
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@OpenForTesting
class ProximityService : DaggerService(), ProximityDetector.ProximityListener {

    private val sensorManager: SensorManager by lazy {
        applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    private val windowManager by lazy {
        applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private var proximityDetector: ProximityDetector? = null

    private var overlay: View? = null

    @SuppressLint("InlinedApi")
    private val overlayFlags = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN)

    private val proximityWakeLock: PowerManager.WakeLock? by lazy {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
            powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, TAG)
        } else {
            null
        }
    }

    // KeyguardLock is deprecated (API 13+) and a no-op on API 26+, kept only for
    // best-effort legacy behavior on old devices.
    @Suppress("DEPRECATION")
    private val keyguardLock: KeyguardManager.KeyguardLock by lazy {
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        keyguardManager.newKeyguardLock(TAG)
    }

    private val keyguardDisableCount: AtomicInteger = AtomicInteger(0)

    private val notificationHelper: NotificationHelper by lazy {
        NotificationHelper(applicationContext)
    }

    private val mainHandler: Handler = Handler(Looper.getMainLooper())
    private val proximityHandler: Handler = Handler(Looper.getMainLooper())

    @Inject
    lateinit var appStorage: AppStorage

    override fun onCreate() {
        super.onCreate()

        proximityDetector = ProximityDetector(this)
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        if (sensor != null) {
            sensorManager.registerListener(proximityDetector, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        } else {
            mainHandler.post { toast(getString(R.string.toast_no_proximity_sensor)) }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            INTENT_ACTION_START -> start()
            INTENT_ACTION_STOP -> stop()
        }
        return Service.START_NOT_STICKY
    }

    override fun onDestroy() {
        if (isRunning) stop()
        sensorManager.unregisterListener(proximityDetector)
    }

    // binding not supported
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onNear() {
        if (isRunning) {
            val delay = when (appStorage.getInt(SCREEN_OFF_DELAY, 0)) {
                0 -> 0L
                1 -> 500L
                2 -> 1000L
                3 -> 1500L
                4 -> 2000L
                5 -> 2500L
                6 -> 3000L
                else -> 0L
            }
            proximityHandler.postDelayed({ updateProximitySensorMode(true) }, delay)
        }
    }

    override fun onFar() {
        if (isRunning) {
            proximityHandler.removeCallbacksAndMessages(null)
            updateProximitySensorMode(false)
        }
    }

    private fun start() {
        proximityWakeLock?.let {
            if (it.isHeld || isRunning) {
                mainHandler.post { toast(getString(R.string.toast_service_already_active)) }
            } else {
                mainHandler.post { toast(getString(R.string.toast_service_started)) }
                startForeground(NOTIFICATION_ID, notificationHelper.getRunningNotification())
                isRunning = true
                ServiceState.setRunning(true)
            }
        } ?: run {
            mainHandler.post { toast(getString(R.string.toast_wakelock_not_supported)) }
        }
    }

    private fun stop() {
        mainHandler.post { toast(getString(R.string.toast_service_stopped)) }
        proximityHandler.removeCallbacksAndMessages(null)
        updateProximitySensorMode(false)
        isRunning = false
        ServiceState.setRunning(false)
        stopForeground(true)
        stopSelf()

        if (!appStorage.getBoolean(NOTIFICATION_DISMISS, true)) {
            notificationHelper.notify(NOTIFICATION_ID, notificationHelper.getStoppedNotification())
        } else {
            // Ensure any stale stopped-notification is cleared when dismissing.
            stopForeground(true)
        }
    }

    private fun updateProximitySensorMode(on: Boolean) {
        when (appStorage.getInt(OPERATIONAL_MODE, Mode.DEFAULT.ordinal)) {
            Mode.DEFAULT.ordinal -> updateDefaultMode(on)
            Mode.AMOLED_WAKELOCK.ordinal -> updateAMOLEDMode(
                on,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
            Mode.AMOLED_NO_WAKELOCK.ordinal -> updateAMOLEDMode(on)
        }
    }

    private fun updateDefaultMode(on: Boolean) {
        // Clean up any AMOLED overlay when switching back to default mode.
        if (overlay != null) {
            updateAMOLEDMode(false)
        }
        proximityWakeLock?.let { wakeLock ->
            synchronized(wakeLock) {
                if (on) {
                    if (!wakeLock.isHeld) {
                        wakeLock.acquire()
                        updateKeyguardMode(false)
                    }
                } else {
                    if (wakeLock.isHeld) {
                        @Suppress("DEPRECATION")
                        wakeLock.release()
                        updateKeyguardMode(true)
                    }
                }
            }
        }
    }

    private fun updateAMOLEDMode(on: Boolean, flags: Int = 0) {
        proximityWakeLock?.let { wakeLock ->
            if (wakeLock.isHeld) {
                @Suppress("DEPRECATION")
                wakeLock.release()
                updateKeyguardMode(true)
            }
        }
        if (on && !checkDrawOverlaySetting()) return
        synchronized(this) {
            if (on) {
                if (overlay == null) {
                    overlay = FrameLayout(this).apply {
                        systemUiVisibility = overlayFlags
                        setTheme(R.style.OverlayTheme)
                    }
                    val params = WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_SYSTEM_ALERT else WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                        flags,
                        PixelFormat.OPAQUE
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        params.layoutInDisplayCutoutMode =
                            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }

                    windowManager.addView(overlay, params)
                    updateKeyguardMode(false)
                }
            } else {
                overlay?.let {
                    it.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
                    windowManager.removeView(it)
                    overlay = null
                }
                updateKeyguardMode(true)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun updateKeyguardMode(on: Boolean) {
        // No-op on API 26+ where KeyguardLock is ignored; keep ref-counted for legacy.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) return
        synchronized(keyguardLock) {
            if (on) {
                if (keyguardDisableCount.get() > 0) {
                    if (keyguardDisableCount.decrementAndGet() == 0) {
                        keyguardLock.reenableKeyguard()
                    }
                }
            } else {
                if (keyguardDisableCount.getAndAdd(1) == 0) {
                    keyguardLock.disableKeyguard()
                }
            }
        }
    }

    private fun checkDrawOverlaySetting(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val canDrawOverlays = Settings.canDrawOverlays(this)
        if (!canDrawOverlays) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            mainHandler.post { toast(getString(R.string.toast_amoled_overlay_permission)) }
        }
        return canDrawOverlays
    }

    private fun toast(text: String) {
        Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "ProximityService:ProximitySensorWakeLock"

        const val INTENT_ACTION_START = "ss.proximityservice.START"
        const val INTENT_ACTION_STOP = "ss.proximityservice.STOP"

        const val NOTIFICATION_ID = 1

        @Volatile
        var isRunning = false
            set(value) {
                field = value
                // Keep observable state in sync for Activities/Tiles.
                // Direct field access from tests stays compatible.
                if (ServiceState.running != value) {
                    ServiceState.setRunning(value)
                }
            }
    }
}
