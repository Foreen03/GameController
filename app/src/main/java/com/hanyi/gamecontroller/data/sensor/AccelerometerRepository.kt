package com.hanyi.gamecontroller.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AccelerometerRepository(
    context: Context
): BaseSensorRepository(
    context,
    Sensor.TYPE_ACCELEROMETER,
    SensorManager.SENSOR_DELAY_FASTEST
) {
    private val _accel = MutableStateFlow(Triple(0f, 0f, 0f))
    val accel: StateFlow<Triple<Float, Float, Float>> = _accel

    override fun onSensorValueChanged(event: SensorEvent) {
        _accel.value = Triple(
            event.values[0],
            event.values[1],
            event.values[2]
        )
    }
}