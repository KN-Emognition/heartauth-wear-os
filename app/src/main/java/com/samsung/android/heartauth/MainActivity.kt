package com.samsung.android.heartauth

import android.Manifest
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samsung.android.heartauth.PermissionHelper.isGranted
import com.samsung.android.heartauth.PermissionHelper.resolveHealthPermission
import com.samsung.android.heartauth.ui.RootScaffold
import com.samsung.android.heartauth.ui.RootViewModel
import com.samsung.android.heartauth.ui.RootViewModelFactory
import com.samsung.android.service.health.tracking.HealthTrackingService
import com.samsung.android.service.health.tracking.data.HealthTrackerType

class MainActivity : ComponentActivity() {

    private lateinit var healthService: HealthServiceManager
    private lateinit var ecgController: EcgMeasurementController


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val neededPermission = resolveHealthPermission(this)
        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (!granted) {
                Toast.makeText(this, getString(R.string.NoPermission), Toast.LENGTH_LONG).show()
            }
        }
        if (!isGranted(this, neededPermission)) {
            permissionLauncher.launch(
                if (neededPermission == getString(R.string.additionalHealthDataPermission)) neededPermission
                else Manifest.permission.BODY_SENSORS
            )
        }

        healthService =
            HealthServiceManager(applicationContext, object : HealthServiceManager.Listener {
                override fun onConnected(service: HealthTrackingService?) {
                    if (!healthService.isTrackerSupported(HealthTrackerType.ECG_ON_DEMAND)) {
                        Toast.makeText(
                            this@MainActivity, getString(R.string.NoECGSupport), Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                }

                override fun onDisconnected() {}
                override fun onFatalError() {
                    finish()
                }
            }).also { it.connect() }

        ecgController = EcgMeasurementController(healthService)

        setContent {
            MaterialTheme {
                val vm: RootViewModel = viewModel(
                    factory = RootViewModelFactory(ecgController)
                )
                RootScaffold(viewModel = vm)
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        ecgController.stop()
        healthService.disconnect()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
