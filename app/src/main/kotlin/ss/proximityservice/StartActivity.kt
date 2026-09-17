package ss.proximityservice

import android.app.Activity

class StartActivity : Activity() {
    // onResume (not onCreate): keeps the trampoline alive slightly longer so the
    // startForegroundService call below stays inside the foreground window on API 31+.
    override fun onResume() {
        super.onResume()
        ServiceStarter.start(this, ProximityService.INTENT_ACTION_START)
        finish()
    }
}
