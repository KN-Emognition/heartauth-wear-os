package com.samsung.android.heartauth

import android.Manifest
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.samsung.android.heartauth.PermissionHelper.isGranted
import com.samsung.android.heartauth.PermissionHelper.resolveHealthPermission
import com.samsung.android.heartauth.ui.MainScreen
import com.samsung.android.heartauth.ui.MeasurementScreen
import com.samsung.android.heartauth.ui.ResultScreen
import com.samsung.android.service.health.tracking.HealthTrackingService
import com.samsung.android.service.health.tracking.data.HealthTrackerType
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var healthService: HealthServiceManager
    private lateinit var ecgController: EcgMeasurementController

    private sealed class ScreenState {
        object Menu : ScreenState()
        data class WaitingForContact(val durationMs: Long) : ScreenState()
        data class Measuring(val durationMs: Long) : ScreenState()
        data class Result(val durationMs: Long, val avgMv: Double, val success: Boolean) : ScreenState()
    }

    private val contactTimeoutMs = 10_000L
    private val ignoreFirstMs = 2_000L
    private val tickMs = 1_000L

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
                if (neededPermission == getString(R.string.additionalHealthDataPermission))
                    neededPermission
                else
                    Manifest.permission.BODY_SENSORS
            )
        }

        healthService = HealthServiceManager(applicationContext, object : HealthServiceManager.Listener {
            override fun onConnected(service: HealthTrackingService?) {
                if (!healthService.isTrackerSupported(HealthTrackerType.ECG_ON_DEMAND)) {
                    Toast.makeText(this@MainActivity, getString(R.string.NoECGSupport), Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            override fun onDisconnected() {}
            override fun onFatalError() { finish() }
        }).also { it.connect() }

        ecgController = EcgMeasurementController(healthService)

        setContent {
            MaterialTheme {
                AppContent(
                    startFlow = { durationMs -> startFlow(durationMs) },
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMeasurementInternal()
        healthService.disconnect()
    }

    // ----- state shared with Compose
    private var screenState by mutableStateOf<ScreenState>(ScreenState.Menu)
    private var statusText by mutableStateOf("")
    private var lastAvg by mutableDoubleStateOf(0.0)
    private var secondsLeft by mutableIntStateOf(0)

    private fun startFlow(durationMs: Long) {
        if (!healthService.isConnected()) {
            Toast.makeText(this, getString(R.string.ConnectionError), Toast.LENGTH_SHORT).show()
            return
        }
        lastAvg = 0.0
        secondsLeft = (durationMs / 1000).toInt()
        statusText = getString(R.string.outputMeasuring)
        screenState = ScreenState.WaitingForContact(durationMs)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // countdown starts only after first valid contact
        ecgController.start(object : EcgMeasurementController.Listener {
            override fun onLeadOff() {
                if (screenState is ScreenState.WaitingForContact || screenState is ScreenState.Measuring) {
                    statusText = getString(R.string.outputWarning)
                }
            }
            override fun onData(avgMv: Double) {
                lastAvg = avgMv
                if (screenState is ScreenState.WaitingForContact && !ecgController.isLeadOff()) {
                    screenState = ScreenState.Measuring((screenState as ScreenState.WaitingForContact).durationMs)
                }
            }
            override fun onErrorPermission() {
                Toast.makeText(this@MainActivity, getString(R.string.NoPermission), Toast.LENGTH_SHORT).show()
            }
            override fun onErrorPolicy() {
                Toast.makeText(this@MainActivity, getString(R.string.SDKPolicyError), Toast.LENGTH_SHORT).show()
            }
            override fun onFinished(success: Boolean, finalAvgMv: Double) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                if (!success) {
                    screenState = ScreenState.Menu
                    Toast.makeText(this@MainActivity, getString(R.string.MeasurementFailed), Toast.LENGTH_SHORT).show()
                } else {
                    val d = when (val s = screenState) {
                        is ScreenState.Measuring -> s.durationMs
                        is ScreenState.WaitingForContact -> s.durationMs
                        else -> 0L
                    }
                    screenState = ScreenState.Result(d, finalAvgMv, true)
                }
            }
        })
    }

    private fun stopMeasurementInternal() {
        ecgController.stop()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        statusText = getString(R.string.outputStart)
        screenState = ScreenState.Menu
    }

    @Composable
    private fun AppContent(startFlow: (Long) -> Unit) {
        val ctx = LocalContext.current

        // WAITING-FOR-CONTACT timeout
        LaunchedEffect(screenState) {
            if (screenState is ScreenState.WaitingForContact) {
                val start = System.currentTimeMillis()
                while (isActive && System.currentTimeMillis() - start < contactTimeoutMs) {
                    if (screenState is ScreenState.Measuring || screenState is ScreenState.Menu) break
                    delay(100)
                }
                if (screenState is ScreenState.WaitingForContact) {
                    stopMeasurementInternal()
                    Toast.makeText(ctx, ctx.getString(R.string.outputWarning), Toast.LENGTH_SHORT).show()
                }
            }
        }
        LaunchedEffect(screenState) {
            if (screenState is ScreenState.Measuring) {
                val total = (screenState as ScreenState.Measuring).durationMs
                val start = System.currentTimeMillis()

                while (isActive) {

                    val elapsed = System.currentTimeMillis() - start
                    val left = (total - elapsed).coerceAtLeast(0)
                    secondsLeft = (left / 1000).toInt()
                    if (left <= 0) break

                    if (!ecgController.isLeadOff()) {
                        statusText =
                            if (elapsed >= ignoreFirstMs) {
                                // use ctx.getString here (NOT stringResource)
                                ctx.getString(
                                    R.string.MeasurementUpdate,
                                    secondsLeft,
                                    String.format(Locale.ENGLISH, "%.2f", lastAvg)
                                )
                            } else {
                                ctx.getString(R.string.outputMeasuring)
                            }
                    } else {
                        statusText = ctx.getString(R.string.outputWarning)
                    }
                    delay(tickMs)
                }
                if (screenState is ScreenState.Measuring) {
                    ecgController.finishFromTimer()
                }
            }
        }

        Surface(Modifier.fillMaxSize()) {
            when (val s = screenState) {
                is ScreenState.Menu -> MainScreen(
                    onMeasure = { startFlow(5_000L) },
                )
                is ScreenState.WaitingForContact -> MeasurementScreen(
                    title = getString(R.string.waiting_for_contact_title),
                    status = statusText,
                )
                is ScreenState.Measuring -> MeasurementScreen(
                    title = getString(R.string.measuring_title, (s.durationMs / 1000)),
                    status = statusText,
                )
                is ScreenState.Result -> ResultScreen(
                    message = stringResource(
                        R.string.MeasurementSuccessful,
                        String.format(Locale.ENGLISH, "%.2f", s.avgMv)
                    ),
                    onBackHome = { screenState = ScreenState.Menu }
                )
            }
        }
    }



}
