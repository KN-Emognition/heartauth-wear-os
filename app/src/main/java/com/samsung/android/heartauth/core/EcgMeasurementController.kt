package com.samsung.android.heartauth.core

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.MainThread
import com.samsung.android.heartauth.Constants
import com.samsung.android.heartauth.utils.HealthServiceManager
import com.samsung.android.service.health.tracking.HealthTracker
import com.samsung.android.service.health.tracking.HealthTracker.TrackerEventListener
import com.samsung.android.service.health.tracking.data.DataPoint
import com.samsung.android.service.health.tracking.data.HealthTrackerType
import com.samsung.android.service.health.tracking.data.ValueKey
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean


class EcgMeasurementController(private val serviceManager: HealthServiceManager, duration: Long) {

    enum class FinishReason { TIMER, LEAD_OFF, CANCELLED }

    interface Listener {
        fun onLeadOff()
        fun onData()
        fun onStableTick()
        fun onProgress(fraction: Float)
        fun onFinished(success: Boolean, samples: List<Float>, finishedReason: FinishReason)
    }

    private val targetSamples: Int =
        ((duration * Constants.ECG_SIGNAL_FREQ) / 1000L).toInt().coerceAtLeast(1)

    @Volatile
    private var contactStableCount = 0

    @Volatile
    private var offStableCount = 0

    @Volatile
    private var isMeasuring = false

    @Volatile
    private var onLeadCount = 0

    private val samples = Collections.synchronizedList(mutableListOf<Float>())
    private val main = Handler(Looper.getMainLooper())
    private val running = AtomicBoolean(false)
    private val leadOff = AtomicBoolean(true)
    private var tracker: HealthTracker? = null
    private var listener: Listener? = null

    @MainThread
    fun isLeadOff() = leadOff.get()

    @MainThread
    fun start(listener: Listener) {
        if (running.get()) return
        this.listener = listener
        if (!serviceManager.isConnected() ||
            !serviceManager.isTrackerSupported(HealthTrackerType.ECG_ON_DEMAND)
        ) {
            Log.w(Constants.HAUTH_TAG, "Service not connected or ECG not supported")
            return
        }
        resetVariables()

        tracker?.unsetEventListener()
        tracker = serviceManager.getTracker(HealthTrackerType.ECG_ON_DEMAND)

        running.set(true)
        tracker!!.setEventListener(eventListener)
    }


    private val eventListener = object : TrackerEventListener {
        override fun onDataReceived(list: List<DataPoint>) {
            if (!running.get() || list.isEmpty()) return

            val lo = list[0].getValue(ValueKey.EcgSet.LEAD_OFF)
            val isOff = (lo == Constants.WATCH_NO_CONTACT_CODE)

            if (isOff) {
                if(isMeasuring){
                    val needed = targetSamples - onLeadCount
                    if (needed <= 0) {
                        finishInternal(success = true, reason = FinishReason.TIMER)
                        return
                    }
                    val toAdd = minOf(needed, list.size)
                    for (i in 0 until toAdd) {
                        val mv = list[i].getValue(ValueKey.EcgSet.ECG_MV)
                        samples.add(mv)
                    }
                    Log.w(Constants.HAUTH_TAG, "ADDED ${toAdd}")
                    onLeadCount += toAdd

                }
                offStableCount++
                contactStableCount = 0
                leadOff.set(true)
                main.post { listener?.onLeadOff() }
                if (isMeasuring && offStableCount >= Constants.ECG_LID_DEBOUNCE_TICKS) {
                    finishInternal(success = false, reason = FinishReason.LEAD_OFF)
                }
                return
            }

            leadOff.set(false)
            offStableCount = 0
            contactStableCount++
            main.post { listener?.onStableTick() }
            if (!isMeasuring && contactStableCount >= Constants.ECG_LID_STARTING_TICKS) {
                isMeasuring = true
                main.post { listener?.onData() }
            } else if (isMeasuring) {
                main.post { listener?.onData() }
            }

            if (!isMeasuring) return

            val needed = targetSamples - onLeadCount
            if (needed <= 0) {
                finishInternal(success = true, reason = FinishReason.TIMER)
                return
            }
            val toAdd = minOf(needed, list.size)
            for (i in 0 until toAdd) {
                val mv = list[i].getValue(ValueKey.EcgSet.ECG_MV)
                samples.add(mv)
            }
            onLeadCount += toAdd

            val fraction = samples.size.toFloat() / targetSamples.toFloat()
            main.post { listener?.onProgress(fraction) }

            if (onLeadCount >= targetSamples) {
                finishInternal(success = true, reason = FinishReason.TIMER)
                return
            }
        }

        override fun onFlushCompleted() {}
        override fun onError(err: HealthTracker.TrackerError) {
            if (!running.get()) return
            Log.w(Constants.HAUTH_TAG, "Tracker error: $err")
        }
    }

    @MainThread
    fun stop() {
        if (running.get()) finishInternal(success = false, reason = FinishReason.CANCELLED)
    }

    @MainThread
    private fun finishInternal(success: Boolean, reason: FinishReason) {
        if (!running.getAndSet(false)) return
        tracker?.unsetEventListener()
        tracker = null
        val all = synchronized(samples) { samples.toList() }
        samples.clear()
        main.post { listener?.onFinished(success, all, reason) }
    }

    private fun resetVariables() {
        isMeasuring = false
        leadOff.set(true)
        contactStableCount = 0
        offStableCount = 0
        onLeadCount = 0
        samples.clear()
    }
}
