package me.eigenein.arduinocar

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.Toast
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import ninja.eigenein.joypad.JoypadView
import ninja.eigenein.joypad.WheelsPower
import java.io.IOException

class MainActivity : Activity(), JoypadView.Listener {

    private val logTag = MainActivity::class.java.simpleName
    private val connectRetriesCount = 5L

    private val outputMessagesSubject = PublishSubject.create<OutputMessage>()
    private val connectionDisposable = CompositeDisposable()

    private lateinit var joypadView: JoypadView
    private lateinit var progressBar: ProgressBar

    private lateinit var deviceNameMenuItem: MenuItem
    private lateinit var vccMenuItem: MenuItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)

        joypadView = findViewById(R.id.joypad_view)
        joypadView.setListener(this)
        progressBar = findViewById(R.id.progress_bar)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        deviceNameMenuItem = menu.findItem(R.id.menu_main_device_name)
        vccMenuItem = menu.findItem(R.id.menu_main_vcc)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_main_connect -> {
                connectionDisposable.clear() // close any existing connection beforehand
                showDevicesDialog { connectTo(it) }
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPause() {
        super.onPause()
        connectionDisposable.clear()
    }

    override fun onUp() {
        outputMessagesSubject.onNext(deprecatedBrakeOutputMessage)
    }

    override fun onMove(distance: Float, dx: Float, dy: Float) {
        val wheelsPower = WheelsPower.wheelsPower(distance, dx, dy)
        outputMessagesSubject.onNext(DeprecatedMoveOutputMessage(wheelsPower.left, wheelsPower.right))
    }

    private fun showDevicesDialog(listener: (BluetoothDevice) -> Unit) {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (!bluetoothAdapter.isEnabled) {
            startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 0)
            return
        }
        val devices = bluetoothAdapter.bondedDevices.sortedBy { it.name }.toTypedArray()
        if (devices.isEmpty()) {
            Toast.makeText(this, R.string.toast_no_paired_devices, Toast.LENGTH_LONG).show()
            return
        }
        showAlertDialog(this) {
            setTitle(R.string.dialog_title_choose_vehicle)
            setCancelable(true)
            setItems(devices.map { it.name }.toTypedArray(), { _, which -> listener(devices[which]) })
        }
    }

    private fun connectTo(device: BluetoothDevice) {
        connectionDisposable.clear() // close any still existing connection
        connectionDisposable.add(
            device.messages(outputMessagesSubject)
                .subscribeOn(Schedulers.newThread())
                .retry(connectRetriesCount)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { onInputMessage(it) },
                    {
                        if (it !is IOException) {
                            throw RuntimeException(it) // non-IO errors shouldn't be suppressed
                        }
                        Log.e(logTag, "Connection to " + device.name + " failed")
                        Toast.makeText(this, getString(R.string.toast_connection_failed, device.name), Toast.LENGTH_SHORT).show()
                        progressBar.gone()
                    },
                    { Log.e(logTag, "Connection stream ended") },
                    {
                        Toast.makeText(this, getString(R.string.toast_connecting, device.name), Toast.LENGTH_SHORT).show()
                        progressBar.show()
                    }
                )
        )
    }

    private fun onInputMessage(message: InputMessage) {
        Log.d(logTag, "Input message: %s".format(message))
        when (message) {
            is ConnectedMessage -> {
                deviceNameMenuItem.title = message.device_name
                Toast.makeText(this, getString(R.string.toast_connected, message.device_name), Toast.LENGTH_SHORT).show()
                progressBar.gone()
                outputMessagesSubject.onNext(deprecatedNoOperationOutputMessage) // FIXME
            }
            is DeprecatedTelemetryMessage -> vccMenuItem.title = "%.2fV".format(message.vcc)
        }
    }
}
