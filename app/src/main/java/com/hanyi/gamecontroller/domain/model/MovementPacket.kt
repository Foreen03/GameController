package com.hanyi.gamecontroller.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class MovementPacket(
    override val packetType: PacketType = PacketType.MOVEMENT,
    override val timestamp: Long,
    val payload: Payload
): BlePacket {
    @Serializable
    data class Payload(
        val steps: Int,
        val stepsCadence: Float,
        val x: Float,
        val y : Float,
        val z: Float,
        val buttons: Map<String, Boolean>
    )
}