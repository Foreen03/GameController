package com.hanyi.gamecontroller.domain.model

import com.google.gson.annotations.SerializedName

enum class PacketType {
    @SerializedName("movement")
    MOVEMENT,
    @SerializedName("action")
    ACTION,
    @SerializedName("command")
    COMMAND
}