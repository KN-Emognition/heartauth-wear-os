package com.samsung.android.ecgmonitor

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsung.android.ecgmonitor.PermissionHelper.isGranted
import com.samsung.android.ecgmonitor.PermissionHelper.resolveHealthPermission
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

        // permissions
        val neededPermission = resolveHealthPermission(this)
        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (!granted) {
                Toast.makeText(this, getString(R.string.NoPermission), Toast.LENGTH_LONG).show()
            }
        }
        if (!isGranted(this, neededPermission)) {
            // just request the resolved permission string
            permissionLauncher.launch(
                if (neededPermission == getString(R.string.additionalHealthDataPermission))
                    neededPermission
                else
                    Manifest.permission.BODY_SENSORS
            )
        }

        // health service / controller
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
                    stopAll = { stopMeasurementInternal() }
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

    // -------------------- COMPOSE UI --------------------
    @Composable
    private fun AppContent(startFlow: (Long) -> Unit, stopAll: () -> Unit) {
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

        // COUNTDOWN once measuring begins (ignore first 2s for display)
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
                is ScreenState.Menu -> MenuScreen(
                    onShort = { startFlow(5_000L) },
                    onLong = { startFlow(10_000L) }
                )
                is ScreenState.WaitingForContact -> MeasuringScreen(
                    title = getString(R.string.waiting_for_contact_title),
                    status = statusText,
                    showStop = true,
                    onStop = { stopAll() }
                )
                is ScreenState.Measuring -> MeasuringScreen(
                    title = getString(R.string.measuring_title, (s.durationMs / 1000)),
                    status = statusText,
                    showStop = true,
                    onStop = { stopAll() }
                )
                is ScreenState.Result -> ResultScreen(
                    // inline success text; no UiTextFormatter
                    avgText = stringResource(
                        R.string.MeasurementSuccessful,
                        String.format(Locale.ENGLISH, "%.2f", s.avgMv)
                    ),
                    detail = getString(R.string.result_length_fmt, (s.durationMs / 1000)),
                    onBackHome = { screenState = ScreenState.Menu }
                )
            }
        }
    }

    @Composable
    private fun MenuScreen(onShort: () -> Unit, onLong: () -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = getString(R.string.app_name), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onShort, modifier = Modifier.fillMaxWidth()) {
                Text(text = getString(R.string.btn_short_5s))
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onLong, modifier = Modifier.fillMaxWidth()) {
                Text(text = getString(R.string.btn_long_10s))
            }
        }
    }

    @Composable
    private fun MeasuringScreen(title: String, status: String, showStop: Boolean, onStop: () -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(16.dp))
            Text(status, fontSize = 18.sp)
            if (showStop) {
                Spacer(Modifier.height(24.dp))
                OutlinedButton(onClick = onStop) { Text(getString(R.string.stop)) }
            }
        }
    }

    @Composable
    private fun ResultScreen(avgText: String, detail: String, onBackHome: () -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = avgText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(text = detail)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onBackHome) { Text(getString(R.string.back_home)) }
        }
    }
}
