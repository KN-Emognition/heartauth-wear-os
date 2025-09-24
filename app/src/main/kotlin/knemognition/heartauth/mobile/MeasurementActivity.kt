package knemognition.heartauth.mobile

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samsung.android.heartauth.R
import com.samsung.android.service.health.tracking.HealthTrackingService
import com.samsung.android.service.health.tracking.data.HealthTrackerType
import knemognition.heartauth.mobile.api.EcgSenderImpl
import knemognition.heartauth.mobile.core.TriggerArgs
import knemognition.heartauth.mobile.core.EcgMeasurementController
import knemognition.heartauth.mobile.core.RootViewModel
import knemognition.heartauth.mobile.ui.RootScaffold
import knemognition.heartauth.mobile.utils.HealthServiceManager
import knemognition.heartauth.mobile.utils.PermissionHelper.isGranted
import knemognition.heartauth.mobile.utils.PermissionHelper.resolveHealthPermission
import knemognition.heartauth.mobile.utils.RootViewModelFactory

class MeasurementActivity : ComponentActivity() {

    private lateinit var healthService: HealthServiceManager
    private lateinit var ecgController: EcgMeasurementController
    private lateinit var args: TriggerArgs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        args = TriggerArgs.from(intent.extras)!!
        Log.i(
            Constants.HAUTH_TAG,
            "MeasurementActivity started with args ${TriggerArgs.from(intent.extras)}"
        )
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
                            this@MeasurementActivity,
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
            }).also { it.connect() }


        setContent {
            MaterialTheme {
                val vm: RootViewModel = viewModel(
                    factory = RootViewModelFactory(
                        EcgMeasurementController(
                            healthService,
                            args.req.measurementDurationMs
                        ), EcgSenderImpl(
                            args.nodeId, this.applicationContext, args.req.id
                        )
                    )
                )
                RootScaffold(viewModel = vm)
            }
        }
    }

    override fun onDestroy() {
        ecgController.stop()
        healthService.disconnect()
        super.onDestroy()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}