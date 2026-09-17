package ss.proximityservice

import android.app.Activity
import android.os.Bundle

class StartActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ServiceStarter.start(this, ProximityService.INTENT_ACTION_START)
        finish()
    }
}
