package com.hanyi.gamecontroller.ui

import MovementPacket
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.hanyi.gamecontroller.data.ble.BleRepository
import com.hanyi.gamecontroller.data.sensor.AccelerometerRepository
import com.hanyi.gamecontroller.data.sensor.SensorCoordinator
import com.hanyi.gamecontroller.data.sensor.StepDetectorRepository
import com.hanyi.gamecontroller.domain.model.ActionPacket
import com.hanyi.gamecontroller.domain.model.BleConnectionState
import com.hanyi.gamecontroller.domain.model.BleUiState
import com.hanyi.gamecontroller.domain.model.CommandPacket
import com.hanyi.gamecontroller.domain.model.READ_CHAR_UUID
import com.hanyi.gamecontroller.domain.model.SERVICE_UUID
import com.hanyi.gamecontroller.domain.model.WRITE_CHAR_UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    private val bleRepository: BleRepository,
    private val sensorCoordinator: SensorCoordinator,
    stepRepo: StepDetectorRepository,
    accelRepo: AccelerometerRepository
): ViewModel() {

    private val _uiState = MutableStateFlow(BleUiState())
    val uiState: StateFlow<BleUiState> = _uiState

    val steps = stepRepo.steps
    val accel = accelRepo.accel

    private val latestSteps = MutableStateFlow(0)
    private val latestAccel = MutableStateFlow(Triple(0f, 0f, 0f))
    private var streamingJob: Job? = null

    init {
        observeBle()
        requestBlePermission()
        requestActivityPermission()

        viewModelScope.launch {
            steps.collect { latestSteps.value = it }
        }
        viewModelScope.launch {
            accel.collect { latestAccel.value = it }
        }
    }

    private fun observeBle(){
        viewModelScope.launch {
            combine(
                bleRepository.connectionState,
                bleRepository.discoveredDevice,
                bleRepository.receivedData
            ) { connection, devices, data ->
                _uiState.value.copy(
                    connectionState = connection,
                    devices = devices,
                    lastReceivedData = data
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    // Permission Intents
    fun requestBlePermission(){
        _uiState.value = _uiState.value.copy(requestBlePermission = true)
    }

    fun requestActivityPermission(){
        _uiState.value = _uiState.value.copy(requestActivityPermission = true)
    }

    fun onBlePermissionResult(granted: Boolean){
        _uiState.value = _uiState.value.copy(
            blePermissionGranted = granted,
            requestBlePermission = false
        )
    }

    fun onActivityPermissionResult(
        granted: Boolean,
        shouldShowRationale: Boolean
    ) {
        _uiState.value = _uiState.value.copy(
            activityPermissionGranted = granted,
            shouldShowRationale = shouldShowRationale,
            requestActivityPermission = false
        )
    }

    fun startScan() = bleRepository.startScan()

    fun stopScan() = bleRepository.stopScan()

    fun connect(device: BluetoothDevice) = bleRepository.connect(device)

    fun disconnect() = bleRepository.disconnect()

    fun sendData(message: String) {
        viewModelScope.launch {
            bleRepository.sendData(
                SERVICE_UUID,
                WRITE_CHAR_UUID,
                Gson().toJson(
                    CommandPacket(
                    timestamp = System.currentTimeMillis(),
                    payload = CommandPacket.Payload(message)
                ))
            )
        }
    }

    fun sendAction(action: String, phase: String){

        val actionPacket = ActionPacket(
            timestamp = System.currentTimeMillis(),
            payload = ActionPacket.Payload(action, phase)
        )

        viewModelScope.launch {
            bleRepository.sendData(
                SERVICE_UUID,
                WRITE_CHAR_UUID,
                Gson().toJson(actionPacket)
            )
        }
    }

    fun readData() {
        bleRepository.readData(
            SERVICE_UUID,
            READ_CHAR_UUID
        )
    }

    override fun onCleared() {
        super.onCleared()
        bleRepository.cleanUp()
    }

    fun startSensors() = sensorCoordinator.startAll()
    fun stopSensors() = sensorCoordinator.stopAll()

    private suspend fun sendMovementPacket() {
        val accelValue = latestAccel.value

        val packet = MovementPacket(
            timestamp = System.currentTimeMillis(),
            payload = MovementPacket.Payload(
                steps = latestSteps.value,
                x = accelValue.first,
                y = accelValue.second,
                z = accelValue.third
            )
        )

        bleRepository.sendData(
            SERVICE_UUID,
            WRITE_CHAR_UUID,
            Gson().toJson(packet)
        )
    }

    fun startStreaming(){
        if(streamingJob != null) return
        streamingJob = viewModelScope.launch{
            tickerFlow(50).collect{
                sendMovementPacket()
            }
        }
    }

    fun stopStreaming(){
        streamingJob?.cancel()
        streamingJob = null
    }

    fun tickerFlow(periodMs: Long) = flow {
        while (true) {
            emit(Unit)
            delay(periodMs)
        }
    }

    fun onGameStart() {
        if (uiState.value.connectionState !== BleConnectionState.CONNECTED) return
        startSensors()
        startStreaming()
    }

    fun onGamePause() {
        sendPauseCommand()
    }

    fun onGameResume() {
        sendResumeCommand()
    }

    fun sendPauseCommand() {
        viewModelScope.launch {
            bleRepository.sendData(
                SERVICE_UUID,
                WRITE_CHAR_UUID,
                Gson().toJson(
                    CommandPacket(
                        timestamp = System.currentTimeMillis(),
                        payload = CommandPacket.Payload("pause")
                    )
                )
            )
        }

        _uiState.update { it.copy(isPaused = true) }
        stopStreaming()
        stopSensors()
    }

    fun sendResumeCommand() {
        viewModelScope.launch {
            bleRepository.sendData(
                SERVICE_UUID,
                WRITE_CHAR_UUID,
                Gson().toJson(
                    CommandPacket(
                        timestamp = System.currentTimeMillis(),
                        payload = CommandPacket.Payload("resume")
                    )
                )
            )
        }

        _uiState.update { it.copy(isPaused = false) }
        startSensors()
        startStreaming()
    }
}