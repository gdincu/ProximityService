package ss.proximityservice.data

import android.hardware.Sensor
import android.hardware.SensorEvent
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ProximityDetectorTest {

    @Mock
    private lateinit var mockListener: ProximityDetector.ProximityListener

    private lateinit var proximityDetector: ProximityDetector

    @Before
    fun init() {
        proximityDetector = ProximityDetector(mockListener)
    }

    @Test
    fun onNear() {
        proximityDetector.onSensorChanged(mockSensorEvent(value = 0f, maxRange = 8f))
        verify(mockListener, only()).onNear()
    }

    @Test
    fun onFar() {
        proximityDetector.onSensorChanged(mockSensorEvent(value = 8f, maxRange = 8f))
        verify(mockListener, only()).onFar()
    }

    @Test
    fun onNear_whenMaxRangeLargerThanThreshold() {
        // Detector treats < min(max, 8f) as near.
        proximityDetector.onSensorChanged(mockSensorEvent(value = 5f, maxRange = 10f))
        verify(mockListener, only()).onNear()
    }

    @Test
    fun onFar_whenAtThreshold() {
        proximityDetector.onSensorChanged(mockSensorEvent(value = 8f, maxRange = 10f))
        verify(mockListener, only()).onFar()
    }

    private fun mockSensorEvent(value: Float, maxRange: Float): SensorEvent {
        val sensorEvent = mock(SensorEvent::class.java)
        try {
            val valuesField = SensorEvent::class.java.getField("values")
            valuesField.isAccessible = true
            valuesField.set(sensorEvent, floatArrayOf(value))
        } catch (e: ReflectiveOperationException) {
            throw AssertionError("Unable to mock SensorEvent.values", e)
        }

        val sensor = mock(Sensor::class.java)
        `when`(sensor.maximumRange).thenReturn(maxRange)
        try {
            val sensorField = SensorEvent::class.java.getField("sensor")
            sensorField.isAccessible = true
            sensorField.set(sensorEvent, sensor)
        } catch (e: ReflectiveOperationException) {
            // Fall back to direct assignment for Robolectric shadows.
            sensorEvent.sensor = sensor
        }

        return sensorEvent
    }
}
