package com.hanyi.gamecontroller.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.hanyi.gamecontroller.domain.model.GamepadEntity

@Database(entities = [GamepadEntity::class], version = 1)
abstract class GamepadDatabase: RoomDatabase() {
    abstract fun gamepadDao(): GamepadDao

    companion object {
        @Volatile
        private var INSTANCE: GamepadDatabase? = null

        fun getInstance(context: Context): GamepadDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GamepadDatabase::class.java,
                    "gamepad_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}