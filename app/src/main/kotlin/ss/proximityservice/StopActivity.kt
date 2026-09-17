package ss.proximityservice

import android.app.Activity
import android.os.Bundle

class StopActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ServiceStarter.start(this, ProximityService.INTENT_ACTION_STOP)
        finish()
    }
}
