package com.samsung.android.heartauth

import android.os.CountDownTimer
import androidx.annotation.MainThread

/**
 * Lightweight wrapper around CountDownTimer with an optional "ignore first N ms" window.
 * No references to Activity/Views to avoid leaks.
 */
class MeasurementTimer(@JvmField val durationMs: Long, val tickMs: Long, val ignoreFirstMs: Long) {
    interface Listener {
        /** Called on the looper used to create/start the timer (main thread in our use).  */
        @MainThread
        fun onTick(millisUntilFinished: Long)

        /** Called when the countdown completes or is canceled with finish semantics.  */
        @MainThread
        fun onFinish()
    }

    private var timer: CountDownTimer? = null

    /** Start a fresh timer. If one is running, it will be canceled first.  */
    @MainThread
    fun start(listener: Listener) {
        cancel()
        timer = object : CountDownTimer(durationMs, tickMs) {
            override fun onTick(millisUntilFinished: Long) {
                // Skip the first "ignore" window (mirrors your previous if-check)
                if (millisUntilFinished > (durationMs - ignoreFirstMs)) return
                listener.onTick(millisUntilFinished)
            }

            override fun onFinish() {
                listener.onFinish()
                timer = null
            }
        }.start()
    }

    /** Cancel the timer if running. Safe to call multiple times.  */
    @MainThread
    fun cancel() {
        if (timer != null) {
            timer!!.cancel()
            timer = null
        }
    }
}
