package ss.proximityservice

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import ss.proximityservice.data.ServiceState

@RequiresApi(Build.VERSION_CODES.N)
class ToggleTileService : TileService() {

    override fun onClick() {
        val activityClass = if (ServiceState.running || ProximityService.isRunning) {
            StopActivity::class.java
        } else {
            StartActivity::class.java
        }
        // Unlock and launch the toggle activity; tile state refreshes in onStartListening.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            unlockAndRun {
                startActivity(Intent(this, activityClass).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        } else {
            startActivity(Intent(this, activityClass).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    override fun onTileAdded() {
        updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        tile.state = if (ServiceState.running || ProximityService.isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }
}
