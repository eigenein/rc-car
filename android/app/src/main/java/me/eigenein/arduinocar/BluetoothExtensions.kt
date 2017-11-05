package me.eigenein.arduinocar

import android.bluetooth.BluetoothDevice
import android.util.Log
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.Subject
import java.util.*

private val logTag = "BluetoothExtensions"
private val serialUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

fun BluetoothDevice.messages(subject: Subject<OutputMessage>): Observable<InputMessage> = Observable.using({
    Log.i(logTag, "Connecting to %s".format(name))
    val socket = this.createRfcommSocketToServiceRecord(serialUUID)
    socket.connect()
    Log.i(logTag, "Connected to %s".format(name))

    val outputSubscription = subject
        .subscribeOn(Schedulers.newThread())
        .subscribe {
            Log.d(logTag, "Output message: %s".format(it))
            socket.outputStream.writeMessage(it)
        }

    Pair(socket, outputSubscription)
}, {
    val (socket, _) = it

    Observable.concat(
        Observable.just(ConnectedMessage(name)),
        Observable.fromIterable(socket.inputStream.messageIterable())
    )
}, {
    val (socket, outputSubscription) = it
    Log.i(logTag, "Closing connection to %s".format(name))
    outputSubscription.dispose()
    socket.close()
})
