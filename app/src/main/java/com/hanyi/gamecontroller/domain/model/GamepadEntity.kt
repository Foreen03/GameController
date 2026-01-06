package com.hanyi.gamecontroller.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gamepads")
data class GamepadEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val json: String,
)