package com.hanyi.gamecontroller.data.controller

import com.google.gson.Gson
import com.hanyi.gamecontroller.data.ble.BleRepository
import com.hanyi.gamecontroller.domain.model.ActionPacket
import com.hanyi.gamecontroller.domain.model.CommandPacket
import com.hanyi.gamecontroller.domain.model.MovementPacket
import com.hanyi.gamecontroller.domain.model.SERVICE_UUID
import com.hanyi.gamecontroller.domain.model.WRITE_CHAR_UUID

class CommandSender(
    private val bleRepository: BleRepository,
    private val gson: Gson = Gson()
) {
    suspend fun sendAction(action: String, phase: String) {
        sendPacket(
            ActionPacket(
                timestamp = System.currentTimeMillis(),
                payload = ActionPacket.Payload(action, phase)
            )
        )
    }

    suspend fun sendCommand(command: String) {
        sendPacket(
            CommandPacket(
                timestamp = System.currentTimeMillis(),
                payload = CommandPacket.Payload(command)
            )
        )
    }

    suspend fun sendMovement(steps: Int, accel: Triple<Float, Float, Float>) {
        val packet = MovementPacket(
            timestamp = System.currentTimeMillis(),
            payload = MovementPacket.Payload(
                steps = steps,
                x = accel.first,
                y = accel.second,
                z = accel.third
            )
        )
        sendPacket(packet)
    }

    private suspend fun sendPacket(packet: Any) {
        bleRepository.sendData(
            SERVICE_UUID,
            WRITE_CHAR_UUID,
            gson.toJson(packet).toByteArray()
        )
    }
}