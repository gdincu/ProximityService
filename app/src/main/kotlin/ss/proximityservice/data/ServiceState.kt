package ss.proximityservice.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * In-process observable service state.
 *
 * Replaces deprecated LocalBroadcastManager for ACTIVE/INACTIVE events.
 * [running] is safe to read from Services/Tiles (no LifecycleOwner needed),
 * [isRunning] is for Activities/Fragments.
 */
object ServiceState {
    private val _isRunning = MutableLiveData(false)

    val isRunning: LiveData<Boolean> = _isRunning

    @Volatile
    var running: Boolean = false
        private set

    @Synchronized
    fun setRunning(value: Boolean) {
        running = value
        _isRunning.postValue(value)
    }

    @Synchronized
    fun resetForTests() {
        running = false
        _isRunning.postValue(false)
    }
}
