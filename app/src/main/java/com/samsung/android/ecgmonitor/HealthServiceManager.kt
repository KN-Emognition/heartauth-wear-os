package com.samsung.android.ecgmonitor;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import com.samsung.android.ecgmonitor.R;
import com.samsung.android.service.health.tracking.ConnectionListener;
import com.samsung.android.service.health.tracking.HealthTracker;
import com.samsung.android.service.health.tracking.HealthTrackerException;
import com.samsung.android.service.health.tracking.HealthTrackingService;
import com.samsung.android.service.health.tracking.data.HealthTrackerType;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class HealthServiceManager {

    public interface Listener {
        void onConnected(HealthTrackingService service);
        void onDisconnected();
        void onFatalError();
    }

    private static final String TAG = "HealthServiceManager";

    private final Context appContext;
    private final Listener listener;

    private HealthTrackingService service;
    private final AtomicBoolean connected = new AtomicBoolean(false);

    public HealthServiceManager(@NonNull Context appContext, @NonNull Listener listener) {
        this.appContext = appContext.getApplicationContext();
        this.listener = listener;
    }

    private final ConnectionListener connectionListener = new ConnectionListener() {
        @Override
        public void onConnectionSuccess() {
            connected.set(true);
            Toast.makeText(appContext, appContext.getString(R.string.ConnectedToHS), Toast.LENGTH_SHORT).show();
            listener.onConnected(service);
        }

        @Override
        public void onConnectionEnded() {
            connected.set(false);
            listener.onDisconnected();
        }

        @Override
        public void onConnectionFailed(HealthTrackerException e) {
            if (e.getErrorCode() == HealthTrackerException.OLD_PLATFORM_VERSION
                    || e.getErrorCode() == HealthTrackerException.PACKAGE_NOT_INSTALLED) {
                Toast.makeText(appContext, appContext.getString(R.string.NoHealthPlatformError), Toast.LENGTH_LONG).show();
            }
            if (e.hasResolution()) {
                // upstream Activity must call e.resolve(activity)
                Log.w(TAG, "Connection failed; resolution required: " + e.getMessage());
            } else {
                Log.e(TAG, "Could not connect: " + e.getMessage());
                Toast.makeText(appContext, appContext.getString(R.string.ConnectionError), Toast.LENGTH_LONG).show();
                listener.onFatalError();
            }
        }
    };

    @MainThread
    public void connect() {
        try {
            service = new HealthTrackingService(connectionListener, appContext);
            service.connectService();
        } catch (Throwable t) {
            Log.e(TAG, "connect error", t);
            listener.onFatalError();
        }
    }

    @MainThread
    public void disconnect() {
        if (service != null) {
            service.disconnectService();
            service = null;
        }
        connected.set(false);
    }

    public boolean isConnected() { return connected.get(); }

    /** Throws if not connected. */
    public HealthTracker getTracker(HealthTrackerType type) {
        if (service == null) throw new IllegalStateException("Service not connected");
        return service.getHealthTracker(type);
    }

    /** Check device capability for a tracker type. */
    public boolean isTrackerSupported(HealthTrackerType type) {
        if (service == null) return false;
        List<HealthTrackerType> types = service.getTrackingCapability().getSupportHealthTrackerTypes();
        return types.contains(type);
    }
}
