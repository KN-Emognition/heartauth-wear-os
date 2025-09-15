package com.samsung.android.ecgmonitor;

import android.os.CountDownTimer;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;

/**
 * Lightweight wrapper around CountDownTimer with an optional "ignore first N ms" window.
 * No references to Activity/Views to avoid leaks.
 */
public final class MeasurementTimer {

    public interface Listener {
        /** Called on the looper used to create/start the timer (main thread in our use). */
        @MainThread
        void onTick(long millisUntilFinished);

        /** Called when the countdown completes or is canceled with finish semantics. */
        @MainThread
        void onFinish();
    }

    private final long durationMs;
    private final long tickMs;
    private final long ignoreFirstMs;

    @Nullable
    private CountDownTimer timer;

    public MeasurementTimer(long durationMs, long tickMs, long ignoreFirstMs) {
        this.durationMs = durationMs;
        this.tickMs = tickMs;
        this.ignoreFirstMs = ignoreFirstMs;
    }

    /** Start a fresh timer. If one is running, it will be canceled first. */
    @MainThread
    public void start(Listener listener) {
        cancel();
        timer = new CountDownTimer(durationMs, tickMs) {
            @Override
            public void onTick(long millisUntilFinished) {
                // Skip the first "ignore" window (mirrors your previous if-check)
                if (millisUntilFinished > (durationMs - ignoreFirstMs)) return;
                listener.onTick(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                listener.onFinish();
                timer = null;
            }
        }.start();
    }

    /** Cancel the timer if running. Safe to call multiple times. */
    @MainThread
    public void cancel() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public long getDurationMs() { return durationMs; }
    public long getTickMs() { return tickMs; }
    public long getIgnoreFirstMs() { return ignoreFirstMs; }
}
