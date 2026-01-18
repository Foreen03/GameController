package com.hanyi.gamecontroller.domain.model

data class PCPacket(
    val type: String,
    val timeStamp: Long,
    val data: String
)