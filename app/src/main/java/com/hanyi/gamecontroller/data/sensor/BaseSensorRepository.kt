package com.hanyi.gamecontroller.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

abstract class BaseSensorRepository(
    context: Context,
    private val sensorType: Int,
    private val delay: Int
): SensorEventListener {

    protected val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    protected val sensor: Sensor? = sensorManager.getDefaultSensor(sensorType)

    fun start(){
        sensor?.let{
            sensorManager.registerListener(this, it, delay)
        }
    }

    fun stop(){
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onSensorChanged(event: SensorEvent) {
        if(event.sensor.type == sensorType){
            onSensorValueChanged(event)
        }
    }

    protected abstract fun onSensorValueChanged(event: SensorEvent)

}