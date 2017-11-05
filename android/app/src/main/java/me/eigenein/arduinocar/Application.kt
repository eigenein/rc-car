package me.eigenein.arduinocar

import android.util.Log
import io.reactivex.plugins.RxJavaPlugins
import java.io.IOException

class Application : android.app.Application() {
    private val logTag = Application::class.java.simpleName

    override fun onCreate() {
        super.onCreate()

        RxJavaPlugins.setErrorHandler {
            if (it is IOException) {
                Log.w(logTag, "Unhandled Rx I/O error: " + it.message)
            } else {
                throw it
            }
        }
    }
}
