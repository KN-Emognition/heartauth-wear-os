package com.samsung.android.ecgmonitor

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.samsung.android.ecgmonitor.PermissionHelper.isGranted
import com.samsung.android.ecgmonitor.PermissionHelper.resolveHealthPermission
import com.samsung.android.ecgmonitor.UiTextFormatter.measuringUpdate
import com.samsung.android.ecgmonitor.UiTextFormatter.success
import com.samsung.android.ecgmonitor.databinding.ActivityMainBinding
import com.samsung.android.service.health.tracking.HealthTrackingService
import com.samsung.android.service.health.tracking.data.HealthTrackerType

class MainActivity : Activity() {
    private var binding: ActivityMainBinding? = null

    private var healthService: HealthServiceManager? = null
    private var ecgController: EcgMeasurementController? = null
    private var timer: MeasurementTimer? = null

    private var permissionGranted = false
    private var permission: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(
            layoutInflater
        )
        setContentView(binding!!.root)

        permission = resolveHealthPermission(this)
        permissionGranted = isGranted(this, permission!!)
        if (!permissionGranted) requestPermissions(arrayOf(permission!!), 0)

        healthService =
            HealthServiceManager(applicationContext, object : HealthServiceManager.Listener {
                override fun onConnected(s: HealthTrackingService?) {
                    if (!healthService!!.isTrackerSupported(HealthTrackerType.ECG_ON_DEMAND)) {
                        Toast.makeText(
                            applicationContext,
                            getString(R.string.NoECGSupport),
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                }

                override fun onDisconnected() {}
                override fun onFatalError() {
                    finish()
                }
            })
        healthService!!.connect()

        ecgController = EcgMeasurementController(healthService!!)
        timer = MeasurementTimer(MEASUREMENT_DURATION.toLong(), MEASUREMENT_TICK.toLong(), 2000)

        binding!!.butStart.setOnClickListener { v: View? -> toggleMeasurement() }

        binding!!.txtOutput.setText(R.string.outputStart)
    }

    private fun toggleMeasurement() {
        if (!permissionGranted) {
            requestPermissions(arrayOf(permission), 0)
            return
        }
        if (!healthService!!.isConnected()) {
            Toast.makeText(
                applicationContext,
                getString(R.string.ConnectionError),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (!ecgController!!.isRunning()) {
            // START
            binding!!.txtOutput.setText(R.string.outputMeasuring)
            binding!!.butStart.setText(R.string.stop)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            ecgController!!.start(object : EcgMeasurementController.Listener {
                override fun onLeadOff() {
                    binding!!.txtOutput.setText(R.string.outputWarning)
                }

                override fun onData(avgMv: Double) {
                    val secsLeft = timer!!.durationMs / 1000
                }

                override fun onErrorPermission() {
                    Toast.makeText(
                        applicationContext,
                        getString(R.string.NoPermission),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onErrorPolicy() {
                    Toast.makeText(
                        applicationContext,
                        getString(R.string.SDKPolicyError),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onFinished(success: Boolean, finalAvgMv: Double) {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    if (!success) {
                        binding!!.txtOutput.setText(R.string.MeasurementFailed)
                    } else {
                        binding!!.txtOutput.text =
                            success(applicationContext, finalAvgMv)
                    }
                    binding!!.butStart.setText(R.string.RepeatMeasurement)
                }
            })

            timer!!.start(object : MeasurementTimer.Listener {
                override fun onTick(millisUntilFinished: Long) {
                    if (ecgController!!.isRunning()) {
                        if (ecgController!!.isLeadOff()) {
                            binding!!.txtOutput.setText(R.string.outputWarning)
                        } else {
                            val text = measuringUpdate(
                                applicationContext,
                                millisUntilFinished / 1000,
                                ecgController!!.avgMv
                            )
                            binding!!.txtOutput.text = text
                        }
                    }
                }

                override fun onFinish() {
                    ecgController!!.finishFromTimer()
                }
            })
        } else {
            // STOP
            ecgController!!.stop()
            timer!!.cancel()
            binding!!.butStart.setText(R.string.start)
            binding!!.txtOutput.setText(R.string.outputStart)
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ecgController!!.stop()
        timer!!.cancel()
        healthService!!.disconnect()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        permissionGranted = true
        for (i in permissions.indices) {
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                if (!shouldShowRequestPermissionRationale(permissions[i])) {
                    Toast.makeText(
                        applicationContext,
                        getString(R.string.PermissionDeniedPermanently),
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        applicationContext,
                        getString(R.string.PermissionDeniedRationale),
                        Toast.LENGTH_LONG
                    ).show()
                }
                permissionGranted = false
                break
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {
        private const val APP_TAG = "ECG Monitor"
        private const val MEASUREMENT_DURATION = 30000
        private const val MEASUREMENT_TICK = 1000
    }
}
