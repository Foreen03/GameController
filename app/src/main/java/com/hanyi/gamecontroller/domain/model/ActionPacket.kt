package com.hanyi.gamecontroller.domain.model

data class ActionPacket(
    val packetType: String = "action",
    val timestamp: Long,
    val payload: Payload
) {
    data class Payload(
        val action: String,
        val phase: String
    )
}