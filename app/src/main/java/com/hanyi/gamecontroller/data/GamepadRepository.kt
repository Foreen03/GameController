package com.hanyi.gamecontroller.data

import android.content.Context
import android.util.Log
import com.hanyi.gamecontroller.data.local.GamepadDao
import com.hanyi.gamecontroller.domain.model.GamepadConfig
import com.hanyi.gamecontroller.domain.model.GamepadEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.google.gson.Gson
import com.hanyi.gamecontroller.data.local.ImageHandler
import kotlinx.coroutines.flow.first

class GamepadRepository(private val context: Context, private val dao: GamepadDao, private val gson: Gson = Gson()) {

    private val imageHandler = ImageHandler(context)

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

        var finalConfig = config
        val bg = config.theme.backgroundImage

        if(bg != null && bg.enabled && (bg.type == "base64" || bg.type == "url")){
            val localPath = imageHandler.saveImage(bg.type, bg.value)
            if(localPath != null){
                val newBg = bg.copy(
                    type = "file",
                    value = localPath
                )
                Log.d("GamepadRepository", localPath)
                finalConfig = config.copy(
                    theme = config.theme.copy(backgroundImage = newBg)
                )
            }
        }

        val json = gson.toJson(finalConfig)
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
