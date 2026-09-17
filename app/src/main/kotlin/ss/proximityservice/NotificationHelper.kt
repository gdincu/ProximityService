package ss.proximityservice

import android.app.*
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import ss.proximityservice.settings.SettingsActivity

class NotificationHelper(context: Context) : ContextWrapper(context) {
    private val manager: NotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.service_state_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }
    }

    fun notify(id: Int, notification: Notification) {
        // Posting (non-FGS) notifications throws SecurityException on API 33+
        // without the runtime permission; skip silently if not granted.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        manager.notify(id, notification)
    }

    fun getRunningNotification(): Notification {
        return NotificationCompat.Builder(baseContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_screen_lock_portrait)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_running))
            .setContentIntent(getActivityIntent<StopActivity>(PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE))
            .addAction(
                R.drawable.ic_settings,
                getString(R.string.notification_action_settings),
                getActivityIntent<SettingsActivity>(PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .build()
    }

    fun getStoppedNotification(): Notification {
        return NotificationCompat.Builder(baseContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_screen_lock_portrait)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_stopped))
            .setContentIntent(getActivityIntent<StartActivity>(PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE))
            .addAction(
                R.drawable.ic_settings,
                getString(R.string.notification_action_settings),
                getActivityIntent<SettingsActivity>(PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setAutoCancel(true)
            .build()
    }

    private inline fun <reified T : Activity> getActivityIntent(flags: Int): PendingIntent {
        return PendingIntent.getActivity(
            baseContext,
            T::class.java.hashCode(),
            Intent(baseContext, T::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            flags
        )
    }

    companion object {
        private const val CHANNEL_ID = "proximityservice"
    }
}
