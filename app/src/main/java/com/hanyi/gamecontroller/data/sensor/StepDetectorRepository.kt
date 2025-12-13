package com.hanyi.gamecontroller.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class StepDetectorRepository(
    context: Context
): BaseSensorRepository(
    context,
    Sensor.TYPE_STEP_DETECTOR,
    SensorManager.SENSOR_DELAY_FASTEST
) {
    private val _steps = MutableStateFlow(0)
    val steps: StateFlow<Int> = _steps
    var stepCount: Int = 0

    override fun onSensorValueChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
            stepCount++
            _steps.tryEmit(stepCount)
        }
    }
}