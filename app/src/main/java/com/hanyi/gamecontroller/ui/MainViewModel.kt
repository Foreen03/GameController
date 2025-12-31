package com.hanyi.gamecontroller.ui

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanyi.gamecontroller.data.ble.BleRepository
import com.hanyi.gamecontroller.data.controller.CommandSender
import com.hanyi.gamecontroller.data.sensor.AccelerometerRepository
import com.hanyi.gamecontroller.data.sensor.SensorCoordinator
import com.hanyi.gamecontroller.data.sensor.StepDetectorRepository
import com.hanyi.gamecontroller.domain.model.BleUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    private val bleRepository: BleRepository,
    private val sensorCoordinator: SensorCoordinator,
    private val commandSender: CommandSender,
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

        if(granted){
            _uiState.value = _uiState.value.copy(
                requestActivityPermission = true
            )
        }
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

    fun sendAction(action: String, phase: String){

        // avoid sending action while in the pause state
        if(_uiState.value.isPaused){
            return
        }

        viewModelScope.launch {
            commandSender.sendAction(action, phase)
        }
    }

//    fun readData() {
//        bleRepository.readData(
//            SERVICE_UUID,
//            READ_CHAR_UUID
//        )
//    }

    override fun onCleared() {
        super.onCleared()
        bleRepository.cleanUp()
    }

    fun startSensors() = sensorCoordinator.startAll()
    fun stopSensors() = sensorCoordinator.stopAll()

    private suspend fun sendMovementPacket() {
        commandSender.sendMovement(
            steps = latestSteps.value,
            accel = latestAccel.value
        )
    }

    fun startStreaming(){
        if(streamingJob != null) return
        streamingJob = viewModelScope.launch{
            tickerFlow(16).collect{
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

    fun sendPauseCommand() {
        viewModelScope.launch {
            commandSender.sendCommand("pause")
        }
        _uiState.update { it.copy(isPaused = true) }
        stopStreaming()
        stopSensors()
    }

    fun sendResumeCommand() {
        viewModelScope.launch {
            commandSender.sendCommand("resume")
        }
        _uiState.update { it.copy(isPaused = false) }
        startSensors()
        startStreaming()
    }
}