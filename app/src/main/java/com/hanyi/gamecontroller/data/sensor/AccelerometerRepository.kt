package com.hanyi.gamecontroller.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.view.Surface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AccelerometerRepository(
    context: Context
) : BaseSensorRepository(
    context,
    Sensor.TYPE_ACCELEROMETER,
    SensorManager.SENSOR_DELAY_FASTEST
) {
    private val displayRotation = DisplayRotationProvider(context)

    private val _accel = MutableStateFlow(Triple(0f, 0f, 0f))
    val accel: StateFlow<Triple<Float, Float, Float>> = _accel

    override fun onSensorValueChanged(event: SensorEvent) {
        val rawX = event.values[0]
        val rawY = event.values[1]
        val rawZ = event.values[2]

        // Remap axes based on screen rotation
        val (x, y, z) = remapAxes(rawX, rawY, rawZ)
        _accel.value = Triple(x, y, z)
    }

    private fun remapAxes(x: Float, y: Float, z: Float): Triple<Float, Float, Float> {
        return when (displayRotation.rotation) {
            Surface.ROTATION_0 -> {
                // Portrait (natural orientation)
                Triple(x, y, z)
            }

            Surface.ROTATION_90 -> {
                // Landscape (home button on right)
                Triple(y, -x, z)
            }

            Surface.ROTATION_180 -> {
                // Portrait upside down
                Triple(-x, -y, z)
            }

            Surface.ROTATION_270 -> {
                // Landscape (home button on left)
                Triple(-y, x, z)
            }

            else -> Triple(x, y, z)
        }
    }
}