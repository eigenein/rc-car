package me.eigenein.arduinocar

import android.bluetooth.BluetoothDevice
import android.util.Log
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

val logTag = "BluetoothExtensions"

fun InputStream.readValue(size: Int): ByteArray = (1..size).map { read().toByte() }.toByteArray()

fun BluetoothDevice.listen(uuid: UUID): Flowable<Message> = Flowable.create({
    val socket = this@listen.createRfcommSocketToServiceRecord(uuid)
    it.setCancellable {
        Log.i(logTag, "Disconnecting from %s".format(this@listen.name))
        socket.close()
    }

    Log.i(logTag, "Connecting to %s".format(this@listen.name))
    it.onNext(ConnectingMessage(this@listen.name))

    try {
        socket.connect()

        Log.i(logTag, "Connected to %s".format(this@listen.name))
        it.onNext(ConnectedMessage(this@listen.name))

        socket.outputStream.write(0x01) // FIXME: NOOP

        while (true) {
            val value = socket.inputStream.readValue(6)
            it.onNext(DeprecatedTelemetry(
                ByteBuffer.wrap(value, 2, 4).order(ByteOrder.LITTLE_ENDIAN).int / 1000.0f)
            )
        }
    } catch (error: IOException) {
        Log.w(logTag, "Connection to %s stopped".format(this@listen.name))
        if (!it.isCancelled) {
            it.onError(error)
        }
    }
}, BackpressureStrategy.ERROR)

