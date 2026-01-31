package com.hanyi.gamecontroller.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.UUID

class ImageHandler(context: Context) {
    private val directory = File(context.filesDir, "backgrounds")

    init {
        if(!directory.exists()) directory.mkdirs()
    }

    suspend fun saveImage(type: String, value: String): String? {
        return withContext(Dispatchers.IO){
            try{
                val fileName = "bg_${UUID.randomUUID()}.jpg"
                val file = File(directory, fileName)

                when (type) {
                    "base64" -> {
                        val decodedBytes = Base64.decode(value, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        FileOutputStream(file).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                        }
                    }
                    "url" -> {
                        val bytes = URL(value).readBytes()
                        file.writeBytes(bytes)
                    }
                    else -> return@withContext null
                }
                return@withContext file.absolutePath
            }
            catch (e: Exception){
                e.printStackTrace()
                return@withContext null
            }
        }
    }
}