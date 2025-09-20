package com.samsung.android.heartauth

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
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class EcgMeasurementController(private val serviceManager: HealthServiceManager) {

    enum class FinishReason { TIMER, LEAD_OFF, CANCELLED }

    data class Sample(val timestampMs: Long, val mv: Float, val leadOff: Boolean)

    interface Listener {
        fun onLeadOff()
        fun onData()
        fun onFinished(success: Boolean, samples: List<Sample>, finishedReason: FinishReason)
    }

    private val samples = Collections.synchronizedList(mutableListOf<Sample>())
    private val main = Handler(Looper.getMainLooper())
    private val running = AtomicBoolean(false)
    private val leadOff = AtomicBoolean(true)

    private var tracker: HealthTracker? = null
    private var listener: Listener? = null

    private var startMs: Long = 0L
    private var hasHadContact = false

    @MainThread fun isLeadOff() = leadOff.get()

    @MainThread
    fun isArmed(): Boolean {
        return hasHadContact
    }

    @MainThread
    fun start(listener: Listener) {
        if (running.get()) return
        this.listener = listener
        if (!serviceManager.isConnected() || !serviceManager.isTrackerSupported(HealthTrackerType.ECG_ON_DEMAND)) {
            Log.w(TAG, "Service not connected or ECG not supported")
            return
        }
        tracker = serviceManager.getTracker(HealthTrackerType.ECG_ON_DEMAND)
        running.set(true)

        startMs = System.currentTimeMillis()
        hasHadContact = false
        samples.clear()

        tracker!!.setEventListener(eventListener)
    }

    private val eventListener = object : TrackerEventListener {
        override fun onDataReceived(list: List<DataPoint>) {
            if (list.isEmpty()) return

            val now = System.currentTimeMillis()
            val lo = list[0].getValue(ValueKey.EcgSet.LEAD_OFF)

            if (lo == NO_CONTACT) {
                leadOff.set(true)
                samples.add(Sample(timestampMs = now, mv = 0f, leadOff = true))
                main.post { listener?.onLeadOff() }

                if (hasHadContact) {
                    finishInternal(success = false, reason = FinishReason.LEAD_OFF)
                }
                return
            } else {
                leadOff.set(false)
                hasHadContact = true
            }

            var sum = 0f
            for (dp in list) {
                val mv = dp.getValue(ValueKey.EcgSet.ECG_MV)
                sum += mv
                samples.add(Sample(timestampMs = now, mv = mv, leadOff = false))
            }
            main.post { listener?.onData() }
        }

        override fun onFlushCompleted() { Log.i(TAG, "flush completed") }
        override fun onError(trackerError: HealthTracker.TrackerError) {
            Log.w(TAG, "Tracker event Listener error")
        }
    }

    @MainThread fun finishFromTimer() { finishInternal(success = !leadOff.get(), reason = FinishReason.TIMER) }

    @MainThread fun stop() { if (running.get()) finishInternal(success = false, reason = FinishReason.CANCELLED) }

    @MainThread
    private fun finishInternal(success: Boolean, reason: FinishReason) {
        if (!running.getAndSet(false)) return
        tracker?.unsetEventListener()
        val out = synchronized(samples) { samples.toList() }
        samples.clear()
        main.post { listener?.onFinished(success, out, reason) }
    }

    companion object {
        private const val TAG = "EcgMeasurementCtrl"
        private const val NO_CONTACT = 5
    }
}
