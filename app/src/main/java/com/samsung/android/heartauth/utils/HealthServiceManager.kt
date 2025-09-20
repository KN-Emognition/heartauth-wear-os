package com.samsung.android.heartauth.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.annotation.MainThread
import com.samsung.android.heartauth.R
import com.samsung.android.service.health.tracking.ConnectionListener
import com.samsung.android.service.health.tracking.HealthTracker
import com.samsung.android.service.health.tracking.HealthTrackerException
import com.samsung.android.service.health.tracking.HealthTrackingService
import com.samsung.android.service.health.tracking.data.HealthTrackerType
import java.util.concurrent.atomic.AtomicBoolean

class HealthServiceManager(appContext: Context, private val listener: Listener) {
    interface Listener {
        fun onConnected(service: HealthTrackingService?)
        fun onDisconnected()
        fun onFatalError()
    }

    private val appContext: Context = appContext.applicationContext

    private var service: HealthTrackingService? = null
    private val connected = AtomicBoolean(false)

    private val connectionListener: ConnectionListener = object : ConnectionListener {
        override fun onConnectionSuccess() {
            connected.set(true)
            Toast.makeText(
                appContext,
                appContext.getString(R.string.ConnectedToHS),
                Toast.LENGTH_SHORT
            ).show()
            listener.onConnected(service)
        }

        override fun onConnectionEnded() {
            connected.set(false)
            listener.onDisconnected()
        }

        override fun onConnectionFailed(e: HealthTrackerException) {
            if (e.errorCode == HealthTrackerException.OLD_PLATFORM_VERSION
                || e.errorCode == HealthTrackerException.PACKAGE_NOT_INSTALLED
            ) {
                Toast.makeText(
                    appContext,
                    appContext.getString(R.string.NoHealthPlatformError),
                    Toast.LENGTH_LONG
                ).show()
            }
            if (e.hasResolution()) {
                Log.w(TAG, "Connection failed; resolution required: " + e.message)
            } else {
                Log.e(TAG, "Could not connect: " + e.message)
                Toast.makeText(
                    appContext,
                    appContext.getString(R.string.ConnectionError),
                    Toast.LENGTH_LONG
                ).show()
                listener.onFatalError()
            }
        }
    }

    @MainThread
    fun connect() {
        try {
            service = HealthTrackingService(connectionListener, appContext)
            service!!.connectService()
        } catch (t: Throwable) {
            Log.e(TAG, "connect error", t)
            listener.onFatalError()
        }
    }

    @MainThread
    fun disconnect() {
        if (service != null) {
            service!!.disconnectService()
            service = null
        }
        connected.set(false)
    }

    fun isConnected(): Boolean {
        return connected.get()
    }

    fun getTracker(type: HealthTrackerType?): HealthTracker {
        checkNotNull(service) { "Service not connected" }
        return service!!.getHealthTracker(type)
    }

    fun isTrackerSupported(type: HealthTrackerType): Boolean {
        if (service == null) return false
        val types = service!!.trackingCapability.supportHealthTrackerTypes
        return types.contains(type)
    }

    companion object {
        private const val TAG = "HealthServiceManager"
    }
}
