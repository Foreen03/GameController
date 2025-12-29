package com.hanyi.gamecontroller.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ActionPacket(
    override val packetType: PacketType = PacketType.ACTION,
    override val timestamp: Long,
    val payload: Payload
): BlePacket {
    @Serializable
    data class Payload(
        val action: String,
        val phase: String
    )
}