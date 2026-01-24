package com.hanyi.gamecontroller.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.sqrt

class StepDetectorRepository(
    context: Context
) : BaseSensorRepository(
    context,
    Sensor.TYPE_ACCELEROMETER,
    SensorManager.SENSOR_DELAY_FASTEST
) {
    private val _steps = MutableStateFlow(0)
    val steps: StateFlow<Int> = _steps

    private var stepCount = 0

    private val gravityAlpha = 0.9f
    private val gravity = FloatArray(3)
    private val linearAcceleration = FloatArray(3)

    private val VELOCITY_RING_SIZE = 50
    private val velocityRing = FloatArray(VELOCITY_RING_SIZE)
    private var velocityIndex = 0

    private var lastStepTimeNs: Long = 0
    private var lastExtremum = 0f
    private var lastDiff = 0f
    private var isUpward = false

    private val STEP_THRESHOLD = 4.0f
    private val STEP_DELAY_NS = 250_000_000L // 250ms (nanoseconds)

    override fun onSensorValueChanged(event: SensorEvent) {
        gravity[0] = gravityAlpha * gravity[0] + (1 - gravityAlpha) * event.values[0]
        gravity[1] = gravityAlpha * gravity[1] + (1 - gravityAlpha) * event.values[1]
        gravity[2] = gravityAlpha * gravity[2] + (1 - gravityAlpha) * event.values[2]

        linearAcceleration[0] = event.values[0] - gravity[0]
        linearAcceleration[1] = event.values[1] - gravity[1]
        linearAcceleration[2] = event.values[2] - gravity[2]

        val currentMagnitude = sqrt(
            (linearAcceleration[0] * linearAcceleration[0] +
                    linearAcceleration[1] * linearAcceleration[1] +
                    linearAcceleration[2] * linearAcceleration[2]).toDouble()
        ).toFloat()

        detectStep(currentMagnitude, event.timestamp)
    }

    private fun detectStep(currentMagnitude: Float, timestampNs: Long) {
        velocityRing[velocityIndex] = currentMagnitude
        velocityIndex = (velocityIndex + 1) % VELOCITY_RING_SIZE

        var velocityEstimate = 0f
        for (v in velocityRing) {
            velocityEstimate += v
        }
        velocityEstimate /= VELOCITY_RING_SIZE

        val diff = currentMagnitude - velocityEstimate

        if (diff > lastDiff && !isUpward) {
            isUpward = true
        } else if (diff < lastDiff && isUpward) {
            isUpward = false

            if (lastExtremum > STEP_THRESHOLD &&
                timestampNs - lastStepTimeNs > STEP_DELAY_NS) {

                stepCount++
                _steps.tryEmit(stepCount)
                lastStepTimeNs = timestampNs
            }
        }

        lastDiff = diff
        lastExtremum = currentMagnitude
    }
}