package me.eigenein.arduinocar

import java.io.OutputStream

interface OutputMessage

data class DeprecatedNoOperationOutputMessage(val unit: Unit) : OutputMessage
data class DeprecatedBrakeOutputMessage(val unit: Unit) : OutputMessage
data class DeprecatedMoveOutputMessage(val left: Float, val right: Float) : OutputMessage

val deprecatedNoOperationOutputMessage = DeprecatedNoOperationOutputMessage(Unit)
val deprecatedBrakeOutputMessage = DeprecatedBrakeOutputMessage(Unit)

fun OutputStream.writeMessage(message: OutputMessage) = write(
    when (message) {
        is DeprecatedNoOperationOutputMessage -> byteArrayOf(0x01)
        is DeprecatedBrakeOutputMessage -> byteArrayOf(0x02)
        is DeprecatedMoveOutputMessage -> byteArrayOf(
            0x03,
            speedToByte(message.left),
            if (message.left < 0f) 1 else 0,
            speedToByte(message.right),
            if (message.right < 0f) 1 else 0
        )
        else -> byteArrayOf()
    }
)

private fun speedToByte(speed: Float) = (Math.abs(speed) * 255f).toByte()
