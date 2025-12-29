package com.hanyi.gamecontroller.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CommandPacket(
    override val packetType: PacketType = PacketType.COMMAND,
    override val timestamp: Long,
    val payload: Payload
): BlePacket {
    @Serializable
    data class Payload(
        val command: String
    )
}

