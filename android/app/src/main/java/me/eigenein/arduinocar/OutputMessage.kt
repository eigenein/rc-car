package me.eigenein.arduinocar

abstract class OutputMessage(val type: Type) {
    enum class Type(val type: Byte) {
        DEPRECATED_NO_OPERATION(1),
        DEPRECATED_BRAKE(2),
        DEPRECATED_MOVE(3)
    }

    fun serialize(): ByteArray {
        val payload = payload()
        return header(payload) + payload
    }

    open fun header(payload: ByteArray) = byteArrayOf(type.type, payload.size.toByte())
    open fun payload() = ByteArray(0)
}

abstract class DeprecatedOutputMessage(type: Type) : OutputMessage(type) {
    // Deprecated messages haven't got the length field.
    override fun header(payload: ByteArray) = byteArrayOf(type.type)
}

class DeprecatedNoOperationOutputMessage : DeprecatedOutputMessage(OutputMessage.Type.DEPRECATED_NO_OPERATION)
