package com.hanyi.gamecontroller.domain.model

sealed interface BlePacket {
    val packetType: PacketType
    val timestamp: Long
}
