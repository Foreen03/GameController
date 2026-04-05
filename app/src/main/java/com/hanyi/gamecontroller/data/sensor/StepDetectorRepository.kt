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

    private val _stepsCadence = MutableStateFlow(0f)
    val stepsCadence: StateFlow<Float> = _stepsCadence

    private var stepCount = 0
    private val recentStepTimestamps = mutableListOf<Long>()

    private val ALPHA = 0.8f
    private val gravity = FloatArray(3)

    private val WINDOW_SIZE = 10
    private val window = FloatArray(WINDOW_SIZE)
    private var windowIndex = 0

    private var lastValue = 0f
    private var lastDirection = 0
    private var lastPeak = 0f
    private var lastValley = 0f

    private val STEP_THRESHOLD = 1.2f
    private val MIN_STEP_INTERVAL_NS = 250_000_000L
    private var lastStepTime = 0L

    private val WINDOW_NS = 1_000_000_000L
    private var smoothedCadence = 0f
    private val ALPHA_CADENCE = 0.2f
    private val IDLE_TIMEOUT_NS = 500_000_000L
    private var lastCadenceUpdateTime = 0L

    override fun onSensorValueChanged(event: SensorEvent) {

        gravity[0] = ALPHA * gravity[0] + (1 - ALPHA) * event.values[0]
        gravity[1] = ALPHA * gravity[1] + (1 - ALPHA) * event.values[1]
        gravity[2] = ALPHA * gravity[2] + (1 - ALPHA) * event.values[2]

        val x = event.values[0] - gravity[0]
        val y = event.values[1] - gravity[1]
        val z = event.values[2] - gravity[2]

        val magnitude = sqrt(x * x + y * y + z * z)

        detectStep(magnitude, event.timestamp)

        val now = event.timestamp
        if (now - lastStepTime > IDLE_TIMEOUT_NS) {
            smoothedCadence *= 0.90f  // smooth decay
            if (smoothedCadence < 1f) {
                smoothedCadence = 0f
            }
            _stepsCadence.tryEmit(smoothedCadence)
        }
    }

    private fun computeRawCadence(now: Long): Float {
        recentStepTimestamps.removeAll { it < now - WINDOW_NS }

        if (recentStepTimestamps.size < 2) return 0f

        return recentStepTimestamps.size * 60f // since 1 sec window
    }

    private fun smoothCadence(raw: Float): Float {
        val alpha = 0.4f // stronger response for games

        smoothedCadence = alpha * raw + (1 - alpha) * smoothedCadence

        return smoothedCadence.coerceIn(0f, 200f)
    }

    private fun applyDeadzone(value: Float): Float {
        return if (value < 5f) 0f else value
    }

    private fun detectStep(value: Float, timestamp: Long) {
        window[windowIndex] = value
        windowIndex = (windowIndex + 1) % WINDOW_SIZE

        var avg = 0f
        for (v in window) avg += v
        avg /= WINDOW_SIZE

        val direction = when {
            avg > lastValue -> 1
            avg < lastValue -> -1
            else -> 0
        }

        if (direction == -1 && lastDirection == 1) {
            lastPeak = lastValue
        } else if (direction == 1 && lastDirection == -1) {
            lastValley = lastValue
        }

        val peakToValley = lastPeak - lastValley

        if (peakToValley > STEP_THRESHOLD &&
            timestamp - lastStepTime > MIN_STEP_INTERVAL_NS
        ) {
            stepCount++
            _steps.tryEmit(stepCount)
            lastStepTime = timestamp
            recentStepTimestamps.add(timestamp)

            val rawCadence = computeRawCadence(timestamp)
            val smooth = smoothCadence(rawCadence)
            val output = applyDeadzone(smooth)

            _stepsCadence.tryEmit(output)
        }

        lastDirection = direction
        lastValue = avg
    }
}