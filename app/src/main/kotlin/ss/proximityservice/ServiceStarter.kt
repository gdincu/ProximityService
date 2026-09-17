package ss.proximityservice

import android.content.Context
import android.content.Intent
import android.os.Build

internal object ServiceStarter {
    fun start(context: Context, action: String) {
        val intent = Intent(context, ProximityService::class.java).setAction(action)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
