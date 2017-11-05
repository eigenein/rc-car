package me.eigenein.arduinocar

interface InputMessage

data class ConnectedMessage(val device_name: String) : InputMessage
data class DeprecatedTelemetryMessage(val vcc: Float) : InputMessage
