package com.hanyi.gamecontroller.ui

import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.hanyi.gamecontroller.data.GamepadRepository
import com.hanyi.gamecontroller.data.ble.BleRepository
import com.hanyi.gamecontroller.data.controller.CommandSender
import com.hanyi.gamecontroller.data.sensor.AccelerometerRepository
import com.hanyi.gamecontroller.data.sensor.SensorCoordinator
import com.hanyi.gamecontroller.data.sensor.StepDetectorRepository
import com.hanyi.gamecontroller.domain.model.BleUiState
import com.hanyi.gamecontroller.domain.model.GamepadConfig
import com.hanyi.gamecontroller.domain.model.NotificationDialogState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    private val bleRepository: BleRepository,
    private val sensorCoordinator: SensorCoordinator,
    private val commandSender: CommandSender,
    private val gamepadRepository: GamepadRepository,
    stepRepo: StepDetectorRepository,
    accelRepo: AccelerometerRepository,
    private val gson: Gson
): ViewModel() {

    private val _uiState = MutableStateFlow(BleUiState())
    val uiState: StateFlow<BleUiState> = _uiState

    val steps = stepRepo.steps
    val accel = accelRepo.accel

    private val latestSteps = MutableStateFlow(0)
    private val latestAccel = MutableStateFlow(Triple(0f, 0f, 0f))
    private var streamingJob: Job? = null
    private val buttonState = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    private val _dialogState = MutableStateFlow(NotificationDialogState())
    val dialogState: StateFlow<NotificationDialogState> = _dialogState

    private val _gamepads = MutableStateFlow<List<GamepadConfig>>(emptyList())
    val gamepads: StateFlow<List<GamepadConfig>> = _gamepads

    init {
        observeBle()
        requestBlePermission()

        viewModelScope.launch {
            steps.collect { latestSteps.value = it }
        }
        viewModelScope.launch {
            accel.collect { latestAccel.value = it }
        }
        viewModelScope.launch {
            gamepadRepository.getAllGamepads().collect { list ->
                _gamepads.value = list
            }
        }
        viewModelScope.launch {
            bleRepository.layoutEvents.collect { config ->
                Log.d("MainViewModel", "Received layout config from bleRepository: ${gson.toJson(config)}")
                onNotificationReceived(
                    title = "New Layout Received",
                    message = "A new custom layout has received from PC!"
                )
                gamepadRepository.insertGamepad(config)
            }
        }
        viewModelScope.launch {
            bleRepository.connectionEvents.collect { state ->
                onNotificationReceived(
                    title = "PC Disconnected",
                    message = "Server Stopped"
                )
                bleRepository.disconnect()
            }
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
            requestActivityPermission()
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

    fun setButton(id: String, pressed: Boolean){
        buttonState.update { it + (id to pressed) }
        viewModelScope.launch {
            sendMovementPacket()
        }
    }

    override fun onCleared() {
        super.onCleared()
        bleRepository.cleanUp()
    }

    fun startSensors() = sensorCoordinator.startAll()
    fun stopSensors() = sensorCoordinator.stopAll()

    private suspend fun sendMovementPacket() {
        commandSender.sendMovement(
            steps = latestSteps.value,
            accel = latestAccel.value,
            buttonState = buttonState.value
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

    fun onNotificationReceived(title: String, message: String){
        viewModelScope.launch {
            _dialogState.value = NotificationDialogState(
                show = true,
                title = title,
                message = message
            )
        }
    }

    fun dismissDialog(){
        _dialogState.value = NotificationDialogState(show = false)
    }
}