package com.hanyi.gamecontroller.data.controller

import com.google.gson.Gson
import com.hanyi.gamecontroller.data.ble.BleRepository
import com.hanyi.gamecontroller.domain.model.BlePacket
import com.hanyi.gamecontroller.domain.model.CommandPacket
import com.hanyi.gamecontroller.domain.model.Constants.SERVICE_UUID
import com.hanyi.gamecontroller.domain.model.Constants.WRITE_CHAR_UUID
import com.hanyi.gamecontroller.domain.model.MovementPacket

class CommandSender(
    private val bleRepository: BleRepository,
    private val gson: Gson = Gson()
) {

    suspend fun sendCommand(command: String) {
        sendPacket(
            CommandPacket(
                timestamp = System.currentTimeMillis(),
                payload = CommandPacket.Payload(command)
            )
        )
    }

    suspend fun sendMovement(steps: Int, stepsCadence: Float, accel: Triple<Float, Float, Float>, buttonState: Map<String, Boolean>) {
        val packet = MovementPacket(
            timestamp = System.currentTimeMillis(),
            payload = MovementPacket.Payload(
                steps = steps,
                stepsCadence = stepsCadence,
                x = accel.first,
                y = accel.second,
                z = accel.third,
                buttons = buttonState
            )
        )
        sendPacket(packet)
    }

    private suspend fun sendPacket(packet: BlePacket) {
        val success = bleRepository.sendData(
            SERVICE_UUID,
            WRITE_CHAR_UUID,
            gson.toJson(packet).toByteArray()
        )
        if (!success) {
            android.util.Log.e("CommandSender", "Failed to send packet: ${gson.toJson(packet)}")
        }
    }
}