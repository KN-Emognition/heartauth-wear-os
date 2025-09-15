package com.samsung.android.ecgmonitor;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import com.samsung.android.service.health.tracking.HealthTracker;
import com.samsung.android.service.health.tracking.data.DataPoint;
import com.samsung.android.service.health.tracking.data.HealthTrackerType;
import com.samsung.android.service.health.tracking.data.ValueKey;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class EcgMeasurementController {

    public interface Listener {
        void onLeadOff();
        void onData(double avgMv);
        void onErrorPermission();
        void onErrorPolicy();
        void onFinished(boolean success, double finalAvgMv);
    }

    private static final String TAG = "EcgMeasurementCtrl";
    private static final int NO_CONTACT = 5;

    private final HealthServiceManager serviceManager;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean leadOff = new AtomicBoolean(true);
    private final AtomicReference<Float> avgEcgMv = new AtomicReference<>(0f);

    private HealthTracker tracker;
    private Listener listener;

    public EcgMeasurementController(HealthServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    @MainThread
    public boolean isRunning() { return running.get(); }
    @MainThread
    public boolean isLeadOff() { return leadOff.get(); }
    public double getAvgMv() { return avgEcgMv.get(); }

    @MainThread
    public void start(@NonNull Listener listener) {
        if (running.get()) return;
        this.listener = listener;

        if (!serviceManager.isConnected() || !serviceManager.isTrackerSupported(HealthTrackerType.ECG_ON_DEMAND)) {
            // Let caller handle UI messaging if needed
            Log.w(TAG, "Service not connected or ECG not supported");
            return;
        }

        tracker = serviceManager.getTracker(HealthTrackerType.ECG_ON_DEMAND);
        running.set(true);
        tracker.setEventListener(eventListener);
    }

    @MainThread
    public void stop() {
        if (!running.get()) return;
        if (tracker != null) tracker.unsetEventListener();
        running.set(false);
    }

    private final HealthTracker.TrackerEventListener eventListener = new HealthTracker.TrackerEventListener() {
        @Override
        public void onDataReceived(@NonNull List<DataPoint> list) {
            if (list.isEmpty()) return;

            int lo = list.get(0).getValue(ValueKey.EcgSet.LEAD_OFF);
            if (lo == NO_CONTACT) {
                leadOff.set(true);
                main.post(() -> { if (listener != null) listener.onLeadOff(); });
                return;
            } else {
                leadOff.set(false);
            }

            float sum = 0f;
            for (DataPoint dp : list) {
                sum += dp.getValue(ValueKey.EcgSet.ECG_MV);
            }
            float avg = sum / list.size();
            avgEcgMv.set(avg);
            main.post(() -> { if (listener != null) listener.onData(avg); });
        }

        @Override
        public void onFlushCompleted() {
            Log.i(TAG, "flush completed");
        }

        @Override
        public void onError(HealthTracker.TrackerError trackerError) {
            Log.i(TAG, "error: " + trackerError);
            if (listener == null) return;
            switch (trackerError) {
                case PERMISSION_ERROR:
                    main.post(listener::onErrorPermission);
                    break;
                case SDK_POLICY_ERROR:
                    main.post(listener::onErrorPolicy);
                    break;
                default:
                    // ignore others for brevity
            }
        }
    };

    /** Call when your timer finishes to finalize state and notify once. */
    @MainThread
    public void finishFromTimer() {
        if (tracker != null) tracker.unsetEventListener();
        boolean success = !leadOff.get();
        double finalAvg = avgEcgMv.get();
        running.set(false);
        if (listener != null) listener.onFinished(success, finalAvg);
    }
}
