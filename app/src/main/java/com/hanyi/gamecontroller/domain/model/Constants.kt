package com.hanyi.gamecontroller.domain.model

import java.util.UUID

object Constants {
    // App
    const val REQUEST_CODE = 100
    // BLE
    val SERVICE_UUID: UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef0")
    val READ_CHAR_UUID: UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef1")
    val WRITE_CHAR_UUID: UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef2")
    val NOTIFY_CHAR_UUID: UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef3")
}