package me.eigenein.arduinocar

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

interface InputMessage

data class ConnectedMessage(val device_name: String) : InputMessage
data class DeprecatedTelemetryMessage(val vcc: Float) : InputMessage

fun InputStream.messageIterable() = object : Iterable<InputMessage> {
    override fun iterator(): Iterator<InputMessage> = object : Iterator<InputMessage> {
        override fun hasNext(): Boolean = true

        override fun next(): InputMessage = DeprecatedTelemetryMessage(
            ByteBuffer.wrap(readValue(6), 2, 4).order(ByteOrder.LITTLE_ENDIAN).int / 1000f
        )
    }
}
