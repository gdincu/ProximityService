package ss.proximityservice

import android.app.Activity
import android.content.Intent
import android.os.Build
import org.robolectric.Shadows.shadowOf

internal fun nextStartedServiceIntent(activity: Activity): Intent? {
    val shadowActivity = shadowOf(activity)
    // Handles both startService() and startForegroundService() across Robolectric versions.
    runCatching { shadowActivity.peekNextStartedService() }.getOrNull()?.let { return it }
    runCatching {
        val method = shadowActivity.javaClass.methods.firstOrNull {
            it.name == "peekNextStartedForegroundService" && it.parameterCount == 0
        }
        @Suppress("UNCHECKED_CAST")
        (method?.invoke(shadowActivity) as Intent?)
    }.getOrNull()?.let { return it }
    runCatching {
        shadowOf(activity.application).peekNextStartedService()
    }.getOrNull()?.let { return it }
    return null
}
