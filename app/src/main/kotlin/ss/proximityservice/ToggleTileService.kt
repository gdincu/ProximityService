package ss.proximityservice

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import androidx.lifecycle.Observer
import ss.proximityservice.data.ServiceState

@RequiresApi(Build.VERSION_CODES.N)
class ToggleTileService : TileService() {

    // Live updates while the QS panel is open; replaces the old broadcast receiver.
    private val runningObserver = Observer<Boolean> { updateTile() }

    override fun onClick() {
        val currentlyRunning = ServiceState.running || ProximityService.isRunning
        val activityClass = if (currentlyRunning) {
            StopActivity::class.java
        } else {
            StartActivity::class.java
        }
        // Optimistic immediate feedback; corrected by runningObserver when the service confirms.
        setTileState(!currentlyRunning)
        // Unlock and launch the toggle activity; tile state refreshes via runningObserver.
        unlockAndRun {
            startActivity(Intent(this, activityClass).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    override fun onTileAdded() {
        updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        ServiceState.isRunning.observeForever(runningObserver)
        updateTile()
    }

    override fun onStopListening() {
        ServiceState.isRunning.removeObserver(runningObserver)
        super.onStopListening()
    }

    private fun updateTile() {
        setTileState(ServiceState.running || ProximityService.isRunning)
    }

    private fun setTileState(running: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }
}
