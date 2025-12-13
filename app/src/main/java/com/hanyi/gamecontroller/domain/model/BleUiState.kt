package com.hanyi.gamecontroller.domain.model

import android.bluetooth.BluetoothDevice

data class BleUiState (
    val connectionState: BleConnectionState = BleConnectionState.DISCONNECTED,
    val devices: List<BluetoothDevice> = emptyList(),
    val lastReceivedData: String = "",
    val blePermissionGranted: Boolean = false,
    val activityPermissionGranted: Boolean = false,
    val requestBlePermission: Boolean = false,
    val requestActivityPermission: Boolean = false,
    val shouldShowRationale: Boolean = false,
    val isPaused: Boolean = true
)