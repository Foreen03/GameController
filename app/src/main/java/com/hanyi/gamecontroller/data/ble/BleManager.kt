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
import androidx.core.content.edit
import com.hanyi.gamecontroller.domain.model.Constants.NOTIFY_CHAR_UUID
import com.hanyi.gamecontroller.domain.model.Constants.SERVICE_UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine

class BleManager(private val context: Context) {
    private val TAG = "BleManager"

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    private var bluetoothGatt: BluetoothGatt? = null

    private val bleScope = CoroutineScope(Dispatchers.Main)

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
    private val preference = context.getSharedPreferences("ble_prefs", Context.MODE_PRIVATE)

    private val _incomingByteCount = MutableStateFlow(0)
    val incomingByteCount: StateFlow<Int> = _incomingByteCount.asStateFlow()

    private var lastDeviceAddress: String?
        get() = preference.getString("last_device", null)
        set(value){
            preference.edit { putString("last_device", value) }
        }

    private fun refreshDeviceCache(gatt: BluetoothGatt): Boolean {
        try {
            val localMethod = gatt.javaClass.getMethod("refresh")
            val result = localMethod.invoke(gatt) as? Boolean ?: false
            if (result) Log.d(TAG, "GATT cache refresh successful")
            else Log.w(TAG, "GATT cache refresh returned false")
            return result
        } catch (e: Exception) {
            Log.e(TAG, "Could not refresh GATT cache: $e")
        }
        return false
    }

    private val scanCallback = object: ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if(ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return

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
        if(ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)!= PackageManager.PERMISSION_GRANTED){
            Log.e(TAG, "Missing BLUETOOTH_SCAN permission")
            return
        }
        _discoveredDevices.value = emptyList()
        bluetoothLeScanner?.startScan(listOf(serverScanFilter), scanSettings, scanCallback)
        Log.d(TAG, "Started BLE Scan")
    }

    fun stopScan() {
        if(ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)!= PackageManager.PERMISSION_GRANTED) return
        bluetoothLeScanner?.stopScan(scanCallback)
        Log.d(TAG, "Stopped BLE Scan")
    }

    // Connecting to device
    private val gattCallback = object: BluetoothGattCallback() {

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when(newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "Connected to GATT Server.")
                    dataBuffer.clear()

                    bleScope.launch {
                        refreshDeviceCache(gatt)

                        Log.d(TAG, "Waiting 1000ms for cache refresh...")
                        delay(1000)

                        if(ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                            Log.d(TAG, "Discovering services now...")
                            gatt.discoverServices()
                        }
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Disconnected from GATT Server")
                    _connectionState.value = BleConnectionState.DISCONNECTED
                    gatt.close()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if(status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Service discovered. Enabling notifications...")
                enableNotifications(gatt, SERVICE_UUID, NOTIFY_CHAR_UUID)
            } else {
                Log.e(TAG, "Service discovery failed: $status")
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
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
            if (status == BluetoothGatt.GATT_SUCCESS) Log.i(TAG, "MTU updated to $mtu")
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
            if(status == BluetoothGatt.GATT_SUCCESS) {
                _receivedData.value = value.decodeToString()
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic?, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "onCharacteristicWrite failed with status: $status")
            }
            pendingWriteContinuation?.let { cont ->
                cont.resume(status == BluetoothGatt.GATT_SUCCESS)
                pendingWriteContinuation = null
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            val chunk = value.decodeToString()
            dataBuffer.append(chunk)
            _incomingByteCount.value = dataBuffer.length
            if (chunk.contains('\u0000')) {
                val fullMessage = dataBuffer.toString().replace("\u0000", "")
                _receivedData.value = fullMessage
                dataBuffer.clear()
                _incomingByteCount.value = 0
            }
        }
    }

    fun connectToDevice(device: BluetoothDevice) {
        if(ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)!= PackageManager.PERMISSION_GRANTED) return

        stopScan()
        _connectionState.value = BleConnectionState.CONNECTING
        bluetoothGatt?.close()
        lastDeviceAddress = device.address

        Log.d(TAG, "Connecting to ${device.address}")
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    fun disconnect() {
        if(ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)!= PackageManager.PERMISSION_GRANTED) return
        _connectionState.value = BleConnectionState.DISCONNECTED
        bluetoothGatt?.disconnect()
    }

    // Reading and Writing Data
    fun readCharacteristic(serviceUuid: UUID, characteristicUUID: UUID) {
        if(ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)!= PackageManager.PERMISSION_GRANTED) return
        val service = bluetoothGatt?.getService(serviceUuid)
        val characteristic = service?.getCharacteristic(characteristicUUID)
        if(characteristic != null) bluetoothGatt?.readCharacteristic(characteristic)
    }

    suspend fun writeCharacteristic(serviceUuid: UUID, characteristicUUID: UUID, data: ByteArray): Boolean = writeMutex.withLock {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return@withLock false

        val gatt = bluetoothGatt ?: run {
            Log.w(TAG, "BluetoothGatt not initialized")
            return@withLock false
        }
        val service = gatt.getService(serviceUuid) ?: run {
            Log.w(TAG, "Service not found: $serviceUuid")
            return@withLock false
        }
        val characteristic = service.getCharacteristic(characteristicUUID) ?: run {
            Log.w(TAG, "Characteristic not found: $characteristicUUID")
            return@withLock false
        }

        return@withLock suspendCancellableCoroutine { continuation ->
            pendingWriteContinuation = continuation

            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            characteristic.value = data

            if (!gatt.writeCharacteristic(characteristic)) {
                Log.e(TAG, "Failed to initiate characteristic write")
                pendingWriteContinuation?.takeIf { it.isActive }?.resume(false)
                pendingWriteContinuation = null
            }

            continuation.invokeOnCancellation {
                Log.w(TAG, "Write operation was cancelled")
                pendingWriteContinuation = null
            }
        }
    }

    fun enableNotifications(gatt: BluetoothGatt, serviceUuid: UUID, characteristicUUID: UUID) {
        if(ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return

        val service = gatt.getService(serviceUuid)
        val characteristic = service?.getCharacteristic(characteristicUUID)

        if(characteristic != null){
            gatt.setCharacteristicNotification(characteristic, true)
            val cccDescriptorUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
            val descriptor = characteristic.getDescriptor(cccDescriptorUuid)
            if (descriptor != null) {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
            } else {
                Log.e(TAG, "CCC Descriptor not found")
                disconnect()
            }
        } else {
            val foundServices = gatt.services.map { it.uuid.toString() }
            Log.e(TAG, "Notify characteristic not found. Available Services: $foundServices")

            disconnect()
        }
    }

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    suspend fun reconnectLastDevice(): Boolean {
        bluetoothGatt?.close()
        bluetoothGatt = null
        _connectionState.value = BleConnectionState.CONNECTING

        Log.d(TAG, "Reconnecting: Starting scan for server...")

        startScan()

        val device = try {
            withTimeout(10000L) {
                _discoveredDevices
                    .filter { it.isNotEmpty() }
                    .map { it.first() }
                    .first()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Reconnection scan timed out (Server not found).")
            stopScan()
            disconnect()
            return false
        }

        stopScan()

        Log.d(TAG, "Reconnection: Found ${device.address}. Connecting...")

        lastDeviceAddress = device.address
        bluetoothGatt = device.connectGatt(context, false, gattCallback)

        return try {
            withTimeout(10000L) {
                _connectionState
                    .filter { it == BleConnectionState.CONNECTED || it == BleConnectionState.DISCONNECTED }
                    .first { state ->
                        if (state == BleConnectionState.DISCONNECTED) throw Exception("Connection failed")
                        state == BleConnectionState.CONNECTED
                    }
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Connection wait timed out or failed: ${e.message}")
            disconnect()
            false
        }
    }

    fun cleanUp() {
        stopScan()
        disconnect()
    }
}