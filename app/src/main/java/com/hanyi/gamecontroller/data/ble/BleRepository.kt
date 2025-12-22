package com.hanyi.gamecontroller.data.ble

import com.hanyi.gamecontroller.domain.model.BleConnectionState
import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

class BleRepository(
    private val bleManager: BleManager
) {
    val connectionState: StateFlow<BleConnectionState>
        get() = bleManager.connectionState

    val discoveredDevice: StateFlow<List<BluetoothDevice>>
        get() = bleManager.discoveredDevices

    val receivedData: StateFlow<String>
        get() = bleManager.receivedData

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

    fun cleanUp() = bleManager.cleanUp()
}