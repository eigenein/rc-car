package me.eigenein.arduinocar

interface Message

data class ConnectedMessage(val name: String) : Message
data class ConnectingMessage(val name: String) : Message
data class DeprecatedTelemetry(val vcc: Float) : Message
