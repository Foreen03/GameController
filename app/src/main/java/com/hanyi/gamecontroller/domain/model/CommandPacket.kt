package com.hanyi.gamecontroller.domain.model

data class CommandPacket(
    val packetType: String = "command",
    val timestamp: Long,
    val payload: Payload
) {
    data class Payload(
        val command: String
    )
}

