package com.hanyi.gamecontroller.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hanyi.gamecontroller.domain.model.GamepadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GamepadDao {
    @Query("SELECT * FROM gamepads")
    fun getAll(): Flow<List<GamepadEntity>>

    @Query("SELECT * FROM gamepads WHERE id =:id LIMIT 1")
    suspend fun getById(id: String): GamepadEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(gamepad: GamepadEntity)
}