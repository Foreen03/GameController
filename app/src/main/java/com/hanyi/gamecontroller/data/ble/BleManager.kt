package com.hanyi.gamecontroller.data.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import android.bluetooth.BluetoothProfile
import androidx.annotation.RequiresPermission
import com.hanyi.gamecontroller.domain.model.BleConnectionState
import com.hanyi.gamecontroller.domain.model.NOTIFY_CHAR_UUID
import com.hanyi.gamecontroller.domain.model.SERVICE_UUID
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import kotlin.coroutines.resume

class BleManager(private val context: Context) {
    private val TAG = "BleManager"

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    private val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner

    private var bluetoothGatt: BluetoothGatt? = null

    private val _connectionState = MutableStateFlow(BleConnectionState.DISCONNECTED)
    val connectionState: StateFlow<BleConnectionState> = _connectionState

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices

    private val _receivedData = MutableStateFlow<String>("")
    val receivedData: StateFlow<String> = _receivedData
    private var pendingWriteContinuation: CancellableContinuation<Boolean>? = null
    private val writeMutex = Mutex()

    var startTime: Long = 0


    // Scanning for devices
    private val scanCallback = object: ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if(ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED) {
                return
            }

            val device = result.device
            Log.d(TAG, "Found device: ${device.name} - ${device.address}")

            val currentList = _discoveredDevices.value.toMutableList()
            if(!currentList.contains(device)){
                currentList.add(device)
                _discoveredDevices.value = currentList
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed with error: $errorCode")
        }
    }

    fun startScan() {
        if(ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            )!= PackageManager.PERMISSION_GRANTED){
            Log.e(TAG, "Missing BLUETOOTH_SCAN permission")
            return
        }

        _discoveredDevices.value = emptyList()

        bluetoothLeScanner?.startScan(scanCallback)
        Log.d(TAG, "Started BLE Scan")
    }

    fun stopScan() {
        if(ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            )!= PackageManager.PERMISSION_GRANTED){
            Log.e(TAG, "Missing BLUETOOTH_SCAN permission")
            return
        }

        bluetoothLeScanner?.stopScan(scanCallback)
        Log.d(TAG, "Stopped BLE Scan")
    }

    // Connecting to device
    private val gattCallback = object: BluetoothGattCallback() {

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when(newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "Connected to GATT Server")
                    _connectionState.value = BleConnectionState.CONNECTED

                    gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)

                    if(ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) == PackageManager.PERMISSION_GRANTED) {
                        gatt.discoverServices()
                    } else {
                        Log.e(TAG, "Missing BLUETOOTH_CONNECT permission after connection.")
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Disconnected from GATT Server")
                    _connectionState.value = BleConnectionState.DISCONNECTED
                    bluetoothGatt?.close()
                    bluetoothGatt = null
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if(status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Service discovered")

                gatt.services.forEach { service ->
                    Log.d(TAG, "Service UUID: ${service.uuid}")
                    service.characteristics.forEach { char ->
                        Log.d(TAG, "Characteristic UUID: ${char.uuid}")
                    }
                }
                enableNotifications(SERVICE_UUID, NOTIFY_CHAR_UUID)
            } else {
                Log.e(TAG, "Service discovery failed: $status")
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if(status == BluetoothGatt.GATT_SUCCESS) {
                val data = value.decodeToString()
                Log.d(TAG, "Read data: $data")
                _receivedData.value = data
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            if(status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Write successful")
            } else{
                Log.d(TAG, "Write failed: $status")
            }
            pendingWriteContinuation?.let { cont ->
                cont.resume(status == BluetoothGatt.GATT_SUCCESS)
                pendingWriteContinuation = null
            }
        }

        // Called when the server notifies us
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            val data = value.decodeToString()
            Log.d(TAG, "Notification received: $data")
            _receivedData.value = data

            val endTime = System.nanoTime()
            val rttNs = endTime - startTime
            val rttMs = rttNs / 1_000_000.0 // Convert nanoseconds to milliseconds

            Log.d("Latency", "Round Trip Time: $rttMs ms")
            Log.d("Latency", "Estimated One-Way: ${rttMs / 2} ms")
        }
    }

    fun connectToDevice(device: BluetoothDevice) {
        if(ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            )!= PackageManager.PERMISSION_GRANTED){
            Log.e(TAG, "Missing BLUETOOTH_CONNECT permission")
            return
        }

        stopScan()
        _connectionState.value = BleConnectionState.CONNECTING

        // close previous connection
        bluetoothGatt?.close()

        bluetoothGatt = device.connectGatt(
            context,
            false,  // connect immediately
            gattCallback
        )

        Log.d(TAG, "Connecting to ${device.address}")
    }

    fun disconnect() {
        if(ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            )!= PackageManager.PERMISSION_GRANTED){
            return
        }

        _connectionState.value = BleConnectionState.DISCONNECTING
        bluetoothGatt?.disconnect()
    }

    // Reading and Writing Data
    fun readCharacteristic(serviceUuid: UUID, characteristicUUID: UUID) {
        if(ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            )!= PackageManager.PERMISSION_GRANTED){
            return
        }

        val service = bluetoothGatt?.getService(serviceUuid)
        val characteristic = service?.getCharacteristic(characteristicUUID)

        if(characteristic != null){
            bluetoothGatt?.readCharacteristic(characteristic)
        } else{
            Log.e(TAG, "Characteristic not found")
        }
    }

    suspend fun writeCharacteristic(
        serviceUuid: UUID,
        characteristicUUID: UUID,
        data: ByteArray
    ): Boolean = writeMutex.withLock {

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) return false

        val gatt = bluetoothGatt ?: return false
        val service = gatt.getService(serviceUuid) ?: return false
        val characteristic = service.getCharacteristic(characteristicUUID) ?: return false

        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE

        characteristic.value = data

        gatt.writeCharacteristic(characteristic)
    }

    fun enableNotifications(serviceUuid: UUID, characteristicUUID: UUID) {
        if(ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            )!= PackageManager.PERMISSION_GRANTED){
            return
        }

        val service = bluetoothGatt?.getService(serviceUuid)
        val characteristic = service?.getCharacteristic(characteristicUUID)

        if(characteristic != null){
            bluetoothGatt?.setCharacteristicNotification(characteristic, true)

            val descriptor = characteristic.getDescriptor(
                UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
            )
            descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            bluetoothGatt?.writeDescriptor(descriptor)

            Log.d(TAG, "Notifications enabled for $characteristicUUID")
        }
    }

    fun isBluetoothEnabled(): Boolean{
        return bluetoothAdapter?.isEnabled == true
    }

    fun cleanUp() {
        stopScan()
        disconnect()
    }
}