package me.eigenein.arduinocar

import android.bluetooth.BluetoothDevice
import android.util.Log
import io.reactivex.Observable
import java.io.InputStream
import java.util.*

private val logTag = "BluetoothExtensions"
private val serialUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

fun InputStream.readValue(size: Int): ByteArray = (1..size).map { read().toByte() }.toByteArray()

fun BluetoothDevice.messages(): Observable<InputMessage> = Observable.using({
    Log.i(logTag, "Connecting to %s".format(name))
    this.createRfcommSocketToServiceRecord(serialUUID)
}, {
    it.connect()
    Log.i(logTag, "Connected to %s".format(name))
    it.outputStream.write(DeprecatedNoOperationOutputMessage().serialize()) // FIXME
    Observable.merge(
        Observable.just(ConnectedMessage(name)),
        Observable.fromIterable(it.inputStream.messageIterable())
    )
}, {
    Log.i(logTag, "Closing connection to %s".format(name))
    it.close()
})
