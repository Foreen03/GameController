package com.hanyi.gamecontroller.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.hanyi.gamecontroller.domain.model.GamepadEntity

@Database(entities = [GamepadEntity::class], version = 1)
abstract class GamepadDatabase: RoomDatabase() {
    abstract fun gamepadDao(): GamepadDao
}