package com.samsung.android.ecgmonitor;

import static android.content.pm.PackageManager.PERMISSION_DENIED;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.samsung.android.ecgmonitor.databinding.ActivityMainBinding;


public class MainActivity extends Activity {

    private static final String APP_TAG = "ECG Monitor";
    private static final int MEASUREMENT_DURATION = 30_000;
    private static final int MEASUREMENT_TICK = 1_000;

    private ActivityMainBinding binding;

    private HealthServiceManager healthService;
    private EcgMeasurementController ecgController;
    private MeasurementTimer timer;

    private boolean permissionGranted = false;
    private String permission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        permission = PermissionHelper.resolveHealthPermission(this);
        permissionGranted = PermissionHelper.isGranted(this, permission);
        if (!permissionGranted) requestPermissions(new String[]{permission}, 0);

        healthService = new HealthServiceManager(getApplicationContext(), new HealthServiceManager.Listener() {
            @Override public void onConnected(com.samsung.android.service.health.tracking.HealthTrackingService s) {
                if (!healthService.isTrackerSupported(com.samsung.android.service.health.tracking.data.HealthTrackerType.ECG_ON_DEMAND)) {
                    Toast.makeText(getApplicationContext(), getString(R.string.NoECGSupport), Toast.LENGTH_LONG).show();
                    finish();
                }
            }
            @Override public void onDisconnected() { }
            @Override public void onFatalError() { finish(); }
        });
        healthService.connect();

        ecgController = new EcgMeasurementController(healthService);
        timer = new MeasurementTimer(MEASUREMENT_DURATION, MEASUREMENT_TICK, 2_000);

        binding.butStart.setOnClickListener(v -> toggleMeasurement());

        binding.txtOutput.setText(R.string.outputStart);
    }

    private void toggleMeasurement() {
        if (!permissionGranted) {
            requestPermissions(new String[]{permission}, 0);
            return;
        }
        if (!healthService.isConnected()) {
            Toast.makeText(getApplicationContext(), getString(R.string.ConnectionError), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!ecgController.isRunning()) {
            // START
            binding.txtOutput.setText(R.string.outputMeasuring);
            binding.butStart.setText(R.string.stop);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            ecgController.start(new EcgMeasurementController.Listener() {
                @Override public void onLeadOff() {
                    binding.txtOutput.setText(R.string.outputWarning);
                }
                @Override public void onData(double avgMv) {
                    long secsLeft = timer.getDurationMs() / 1000;
                }
                @Override public void onErrorPermission() {
                    Toast.makeText(getApplicationContext(), getString(R.string.NoPermission), Toast.LENGTH_SHORT).show();
                }
                @Override public void onErrorPolicy() {
                    Toast.makeText(getApplicationContext(), getString(R.string.SDKPolicyError), Toast.LENGTH_SHORT).show();
                }
                @Override public void onFinished(boolean success, double finalAvgMv) {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    if (!success) {
                        binding.txtOutput.setText(R.string.MeasurementFailed);
                    } else {
                        binding.txtOutput.setText(UiTextFormatter.success(getApplicationContext(), finalAvgMv));
                    }
                    binding.butStart.setText(R.string.RepeatMeasurement);
                }
            });

            timer.start(new MeasurementTimer.Listener() {
                @Override public void onTick(long millisUntilFinished) {
                    if (ecgController.isRunning()) {
                        if (ecgController.isLeadOff()) {
                            binding.txtOutput.setText(R.string.outputWarning);
                        } else {
                            String text = UiTextFormatter.measuringUpdate(
                                    getApplicationContext(),
                                    millisUntilFinished / 1000,
                                    ecgController.getAvgMv()
                            );
                            binding.txtOutput.setText(text);
                        }
                    }
                }
                @Override public void onFinish() {
                    ecgController.finishFromTimer();
                }
            });

        } else {
            // STOP
            ecgController.stop();
            timer.cancel();
            binding.butStart.setText(R.string.start);
            binding.txtOutput.setText(R.string.outputStart);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ecgController.stop();
        timer.cancel();
        healthService.disconnect();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionGranted = true;
        for (int i = 0; i < permissions.length; ++i) {
            if (grantResults[i] == PERMISSION_DENIED) {
                if (!shouldShowRequestPermissionRationale(permissions[i])) {
                    Toast.makeText(getApplicationContext(), getString(R.string.PermissionDeniedPermanently), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.PermissionDeniedRationale), Toast.LENGTH_LONG).show();
                }
                permissionGranted = false;
                break;
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
