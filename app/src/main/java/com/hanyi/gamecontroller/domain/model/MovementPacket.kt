data class MovementPacket(
    val packetType: String = "movement",
    val timestamp: Long,
    val payload: Payload
) {
    data class Payload(
        val steps: Int,
        val x: Float,
        val y : Float,
        val z: Float
    )
}
