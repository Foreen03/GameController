package com.hanyi.gamecontroller.ui

import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.hanyi.gamecontroller.data.GamepadRepository
import com.hanyi.gamecontroller.data.SystemInterruptionRepository
import com.hanyi.gamecontroller.data.ble.BleRepository
import com.hanyi.gamecontroller.data.controller.CommandSender
import com.hanyi.gamecontroller.data.sensor.AccelerometerRepository
import com.hanyi.gamecontroller.data.sensor.SensorCoordinator
import com.hanyi.gamecontroller.data.sensor.StepDetectorRepository
import com.hanyi.gamecontroller.domain.model.BleConnectionState
import com.hanyi.gamecontroller.domain.model.BleUiState
import com.hanyi.gamecontroller.domain.model.GamepadConfig
import com.hanyi.gamecontroller.domain.model.NotificationDialogState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive

class MainViewModel(
    private val bleRepository: BleRepository,
    private val sensorCoordinator: SensorCoordinator,
    private val commandSender: CommandSender,
    private val gamepadRepository: GamepadRepository,
    stepRepo: StepDetectorRepository,
    accelRepo: AccelerometerRepository,
    private val gson: Gson,
    private val systemInterruptionRepository: SystemInterruptionRepository
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

    private val _progressState = MutableStateFlow(NotificationDialogState())
    val progressState: StateFlow<NotificationDialogState> = _progressState

    private val _isReconnecting = MutableStateFlow(false)
    val isReconnecting: StateFlow<Boolean> = _isReconnecting

    private val _gamepads = MutableStateFlow<List<GamepadConfig>>(emptyList())
    val gamepads: StateFlow<List<GamepadConfig>> = _gamepads

    private var lastHeartbeatTime = 0L
    private val HEARTBEAT_TIMEOUT = 6000L
    private val RECONNECT_DELAY_MS = 2000L
    private val MAX_RETRY_DELAY_MS = 10_000L
    private var reconnectDelay = RECONNECT_DELAY_MS
    private var reconnectJob: Job? = null
    private var heartbeatJob: Job? = null
    private var selectedDisconnect = false

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
                Log.d("MainViewModel", "Received layout config: ${gson.toJson(config)}")
                lastHeartbeatTime = System.currentTimeMillis()
                onNotificationReceived(
                    title = "New Layout Received",
                    message = "A new custom layout has received from PC!"
                )
                gamepadRepository.insertGamepad(config)
            }
        }

        viewModelScope.launch {
            bleRepository.connectionState.collect { state ->
                when (state) {
                    BleConnectionState.CONNECTED -> {
                        startHeartbeatWatchdog()
                        selectedDisconnect = false
                        reconnectDelay = RECONNECT_DELAY_MS
                    }
                    BleConnectionState.DISCONNECTED -> {
                        stopHeartbeatWatchdog()
                    }
                    else -> Unit
                }
            }
        }

        viewModelScope.launch {
            bleRepository.heartbeatEvent.collect { time ->
                lastHeartbeatTime = time
            }
        }

        viewModelScope.launch {
            bleRepository.transferProgress.collect { progress ->
                if (progress > 0f && progress < 1f) {
                    Log.e("Progress", progress.toString())
                    // Show Progress Dialog
                    _dialogState.value = NotificationDialogState(
                        show = true,
                        title = "Downloading Layout...",
                        message = "${(progress * 100).toInt()}%",
                        progress = progress
                    )
                } else if (progress == 0f && _dialogState.value.progress != null) {
                    dismissProgress()
                }
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
            observeSystemInterruptions()
        }
    }

    private var interruptionJob: Job? = null
    private fun observeSystemInterruptions() {
        interruptionJob?.cancel()
        interruptionJob = viewModelScope.launch {
            systemInterruptionRepository.observeInterruption().collect {
                if (!uiState.value.isPaused) {
                    sendPauseCommand()
                    onNotificationReceived(
                        "System Interruption",
                        "Game paused due to incoming call or alarm."
                    )
                }
            }
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

    fun startScan() {
        // Safe call: BleManager usually handles the check internally, but we can catch just in case
        try {
            bleRepository.startScan()
        } catch (e: SecurityException) {
            Log.e("MainViewModel", "Permission denied for startScan")
            requestBlePermission()
        }
    }

    fun stopScan() {
        try {
            bleRepository.stopScan()
        } catch (e: SecurityException) {
            Log.e("MainViewModel", "Permission denied for stopScan")
        }
    }

    fun connect(device: BluetoothDevice) {
        try {
            bleRepository.connect(device)
        } catch (e: SecurityException) {
            Log.e("MainViewModel", "Permission denied for connect")
            requestBlePermission()
        }
    }

    fun disconnect() {
        selectedDisconnect = true
        stopHeartbeatWatchdog()
        reconnectJob?.cancel()
        _isReconnecting.value = false
        dismissDialog()
        try {
            bleRepository.disconnect()
        } catch (e: SecurityException) {
            Log.e("MainViewModel", "Permission denied for disconnect")
        }
    }

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

    private fun handlePcDisconnected() {
        if(selectedDisconnect) return
        if (_uiState.value.connectionState == BleConnectionState.DISCONNECTED) return
        lastHeartbeatTime = 0L

        try {
            bleRepository.disconnect()
        } catch (e: SecurityException) {
            Log.e("MainViewModel", "Cannot disconnect: Permission denied")
        }

        _isReconnecting.value = true
        reconnectBle()
    }

    private fun reconnectBle() {
        if (selectedDisconnect) return
        if (reconnectJob?.isActive == true) return

        reconnectJob = viewModelScope.launch {
            delay(500)

            while (isActive) {
                Log.d("BLE", "Reconnecting loop iteration...")

                try {
                    val success = bleRepository.reconnect()
                    if (success) {
                        Log.d("BLE", "Reconnect successful")
                        _isReconnecting.value = false
                        dismissDialog()
                        return@launch
                    }
                } catch (e: SecurityException) {
                    Log.e("MainViewModel", "Reconnect failed: Permission denied")
                    _isReconnecting.value = false
                    requestBlePermission()
                    return@launch
                }

                Log.d("BLE", "Reconnect attempt failed, retrying in $reconnectDelay ms")
                delay(reconnectDelay)

                reconnectDelay = minOf(
                    reconnectDelay * 2,
                    MAX_RETRY_DELAY_MS
                )
            }
        }
    }

    private fun startHeartbeatWatchdog() {
        heartbeatJob?.cancel()
        heartbeatJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)

                if (lastHeartbeatTime == 0L) continue

                val isTransferring = bleRepository.transferProgress.value > 0f

                if (isTransferring) {
                    // PC is active (sending data), so we manually update the
                    // lastHeartbeatTime to "now" to keep the connection alive.
                    lastHeartbeatTime = System.currentTimeMillis()
                    continue
                }

                val now = System.currentTimeMillis()
                if (now - lastHeartbeatTime > HEARTBEAT_TIMEOUT) {
                    handlePcDisconnected()
                    return@launch
                }
            }
        }
    }

    private fun stopHeartbeatWatchdog() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    fun dismissDialog(){
        _dialogState.value = NotificationDialogState(show = false)
    }

    fun dismissProgress(){
        _progressState.value = NotificationDialogState(show = false)
    }
}