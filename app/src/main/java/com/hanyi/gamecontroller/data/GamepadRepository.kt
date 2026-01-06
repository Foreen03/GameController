package com.hanyi.gamecontroller.data

import com.hanyi.gamecontroller.data.local.GamepadDao
import com.hanyi.gamecontroller.domain.model.GamepadConfig
import com.hanyi.gamecontroller.domain.model.GamepadEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.google.gson.Gson
import kotlinx.coroutines.flow.first

class GamepadRepository(private val dao: GamepadDao, private val gson: Gson = Gson()) {

    fun getAllGamepads(): Flow<List<GamepadConfig>> =
        dao.getAll().map { entities ->
            entities.map { entity ->
                gson.fromJson(entity.json, GamepadConfig::class.java)
            }
        }

    // Get gamepad by id
    suspend fun getGamepadById(id: String): GamepadConfig? {
        val entity = dao.getById(id)
        return entity.let { gson.fromJson(it.json, GamepadConfig::class.java) }
    }

    // Insert or update gamepad
    suspend fun insertGamepad(config: GamepadConfig) {
        val json = gson.toJson(config)
        dao.insert(
            GamepadEntity(
                id = config.gamepad.id,
                name = config.gamepad.name,
                json = json
            )
        )
    }

    suspend fun hasAnyGamepad(): Boolean = dao.getAll().first().isNotEmpty()

}
