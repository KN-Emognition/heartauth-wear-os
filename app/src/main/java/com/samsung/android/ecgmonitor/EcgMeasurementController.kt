package com.samsung.android.ecgmonitor

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.MainThread
import com.samsung.android.service.health.tracking.HealthTracker
import com.samsung.android.service.health.tracking.HealthTracker.TrackerError
import com.samsung.android.service.health.tracking.HealthTracker.TrackerEventListener
import com.samsung.android.service.health.tracking.data.DataPoint
import com.samsung.android.service.health.tracking.data.HealthTrackerType
import com.samsung.android.service.health.tracking.data.ValueKey
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class EcgMeasurementController(private val serviceManager: HealthServiceManager) {
    interface Listener {
        fun onLeadOff()
        fun onData(avgMv: Double)
        fun onErrorPermission()
        fun onErrorPolicy()
        fun onFinished(success: Boolean, finalAvgMv: Double)
    }

    private val main = Handler(Looper.getMainLooper())
    private val running = AtomicBoolean(false)
    private val leadOff = AtomicBoolean(true)
    private val avgEcgMv = AtomicReference(0f)

    private var tracker: HealthTracker? = null
    private var listener: Listener? = null

    @MainThread
    fun isRunning(): Boolean {
        return running.get()
    }

    @MainThread
    fun isLeadOff(): Boolean {
        return leadOff.get()
    }

    val avgMv: Double
        get() = avgEcgMv.get().toDouble()

    @MainThread
    fun start(listener: Listener) {
        if (running.get()) return
        this.listener = listener

        if (!serviceManager.isConnected() || !serviceManager.isTrackerSupported(HealthTrackerType.ECG_ON_DEMAND)) {
            // Let caller handle UI messaging if needed
            Log.w(TAG, "Service not connected or ECG not supported")
            return
        }

        tracker = serviceManager.getTracker(HealthTrackerType.ECG_ON_DEMAND)
        running.set(true)
        tracker!!.setEventListener(eventListener)
    }

    @MainThread
    fun stop() {
        if (!running.get()) return
        if (tracker != null) tracker!!.unsetEventListener()
        running.set(false)
    }

    private val eventListener: TrackerEventListener = object : TrackerEventListener {
        override fun onDataReceived(list: List<DataPoint>) {
            if (list.isEmpty()) return

            val lo = list[0].getValue(ValueKey.EcgSet.LEAD_OFF)
            if (lo == NO_CONTACT) {
                leadOff.set(true)
                main.post { if (listener != null) listener!!.onLeadOff() }
                return
            } else {
                leadOff.set(false)
            }

            var sum = 0f
            for (dp in list) {
                sum += dp.getValue(ValueKey.EcgSet.ECG_MV)
            }
            val avg = sum / list.size
            avgEcgMv.set(avg)
            main.post { if (listener != null) listener!!.onData(avg.toDouble()) }
        }

        override fun onFlushCompleted() {
            Log.i(TAG, "flush completed")
        }

        override fun onError(trackerError: TrackerError) {
            Log.i(TAG, "error: $trackerError")
            if (listener == null) return
            when (trackerError) {
                TrackerError.PERMISSION_ERROR -> main.post { listener!!.onErrorPermission() }
                TrackerError.SDK_POLICY_ERROR -> main.post { listener!!.onErrorPolicy() }
                else -> {}
            }
        }
    }

    /** Call when your timer finishes to finalize state and notify once.  */
    @MainThread
    fun finishFromTimer() {
        if (tracker != null) tracker!!.unsetEventListener()
        val success = !leadOff.get()
        val finalAvg = avgEcgMv.get().toDouble()
        running.set(false)
        if (listener != null) listener!!.onFinished(success, finalAvg)
    }

    companion object {
        private const val TAG = "EcgMeasurementCtrl"
        private const val NO_CONTACT = 5
    }
}
