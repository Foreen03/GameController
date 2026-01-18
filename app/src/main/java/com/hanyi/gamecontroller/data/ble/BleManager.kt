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
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.os.ParcelUuid
import kotlinx.coroutines.flow.asStateFlow
import java.lang.StringBuilder


class BleManager(private val context: Context) {
    private val TAG = "BleManager"

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    private val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner

    private var bluetoothGatt: BluetoothGatt? = null

    private val _connectionState = MutableStateFlow(BleConnectionState.DISCONNECTED)
    val connectionState: StateFlow<BleConnectionState> = _connectionState

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices

    private val serverScanFilter = ScanFilter.Builder().setServiceUuid(ParcelUuid(SERVICE_UUID)).build()
    private val scanSettings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
    private val _receivedData = MutableStateFlow<String>("")
    val receivedData: StateFlow<String> = _receivedData.asStateFlow()
    private var pendingWriteContinuation: CancellableContinuation<Boolean>? = null
    private val dataBuffer = StringBuilder()
    private val writeMutex = Mutex()

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

        bluetoothLeScanner?.startScan(
            listOf(serverScanFilter),
            scanSettings,
            scanCallback
        )
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
                    Log.d(TAG, "Connected to GATT Server. Discovering services...")
                    // When a new connection is made, clear the old buffer
                    dataBuffer.clear()

                    // This logic is correct, discover services after connecting
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
                Log.d(TAG, "Service discovered. Enabling notifications...")
                enableNotifications(SERVICE_UUID, NOTIFY_CHAR_UUID)
            } else {
                Log.e(TAG, "Service discovery failed: $status")
            }
        }

        // 2. Add onDescriptorWrite to confirm notifications are enabled
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "SUCCESS: Descriptor written. Notifications are ON.")
                _connectionState.value = BleConnectionState.CONNECTED

                gatt?.requestMtu(512)
            } else {
                Log.e(TAG, "FAILURE: Descriptor write failed: $status")
                disconnect()
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "MTU updated to $mtu")
            } else {
                Log.w(TAG, "MTU change failed: $status")
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

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            val chunk = value.decodeToString()
            Log.d(TAG, "Chunk received: $chunk")

            dataBuffer.append(chunk)

            if (chunk.contains('\u0000')) {
                Log.d(TAG, "End of transmission detected.")

                val fullMessage = dataBuffer.toString().replace("\u0000", "")

                Log.d(TAG, "Full message reassembled: $fullMessage")

                _receivedData.value = fullMessage

                dataBuffer.clear()
            }
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

            // Standard Client Characteristic Configuration Descriptor UUID
            val cccDescriptorUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
            val descriptor = characteristic.getDescriptor(cccDescriptorUuid)

            if (descriptor != null) {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                Log.d(TAG, "Writing ENABLE_NOTIFICATION_VALUE to descriptor...")
                bluetoothGatt?.writeDescriptor(descriptor)
            } else {
                Log.e(TAG, "CCC Descriptor not found for characteristic $characteristicUUID")
            }
        } else {
            Log.e(TAG, "Notify characteristic not found.")
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