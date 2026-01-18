package com.hanyi.gamecontroller.data.ble

import com.hanyi.gamecontroller.domain.model.BleConnectionState
import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.activity.result.launch
import com.google.gson.Gson
import com.hanyi.gamecontroller.domain.model.BlePacket
import com.hanyi.gamecontroller.domain.model.BleUiState
import com.hanyi.gamecontroller.domain.model.GamepadConfig
import com.hanyi.gamecontroller.domain.model.PCPacket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.UUID

class BleRepository(
    private val bleManager: BleManager,
    private val gson: Gson
) {
    val connectionState: StateFlow<BleConnectionState>
        get() = bleManager.connectionState

    val discoveredDevice: StateFlow<List<BluetoothDevice>>
        get() = bleManager.discoveredDevices

    val receivedData: StateFlow<String>
        get() = bleManager.receivedData

    private val _layoutEvents = MutableSharedFlow<GamepadConfig>(replay = 1)
    val layoutEvents = _layoutEvents.asSharedFlow()

    private val _connectionEvents = MutableSharedFlow<BleConnectionState>(replay = 1)
    val connectionEvents = _connectionEvents.asSharedFlow()

    init {
        observeIncoming()
    }

    fun startScan() = bleManager.startScan()

    fun stopScan() = bleManager.stopScan()

    fun connect(device: BluetoothDevice) = bleManager.connectToDevice(device)

    fun disconnect() = bleManager.disconnect()

    suspend fun sendData(
        serviceUUID: UUID,
        charUUID: UUID,
        data: ByteArray
    ): Boolean {
        return bleManager.writeCharacteristic(
            serviceUuid = serviceUUID,
            characteristicUUID = charUUID,
            data = data
        )
    }

    fun readData(
        serviceUUID: UUID,
        readUUID: UUID
    ){
        return bleManager.readCharacteristic(
            serviceUuid = serviceUUID,
            characteristicUUID = readUUID
        )
    }

    private fun observeIncoming() {
        bleManager.receivedData
            .onEach { json ->
                if(json.isNotBlank()){
                    Log.d("BleRepository", "Received full packet: $json")
                    handleIncoming(json)
                }
            }.launchIn(CoroutineScope(Dispatchers.IO))
    }

    private fun handleIncoming(raw: String) {
        try {
            val packet = gson.fromJson(raw, PCPacket::class.java)

            when (packet.type) {
                "GAMEPAD_LAYOUT" -> {
                    val layoutJson = packet.data
                    val layout = gson.fromJson(
                        layoutJson,
                        GamepadConfig::class.java
                    )
                    CoroutineScope(Dispatchers.IO).launch {
                        _layoutEvents.emit(layout)
                    }
                }

                "PC_DISCONNECTED" -> {
                    val state = BleConnectionState.DISCONNECTED
                    CoroutineScope(Dispatchers.IO).launch {
                        _connectionEvents.emit(state)
                    }
                }

                else -> {
                    Log.w("BLE", "Unknown packet type: ${packet.type}")
                }
            }
        } catch (e: Exception) {
            Log.e("BLE", "Failed to parse incoming packet", e)
        }
    }

    fun cleanUp() = bleManager.cleanUp()
}