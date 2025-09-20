package com.samsung.android.heartauth.ui

import android.app.Activity
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samsung.android.heartauth.EcgMeasurementController
import com.samsung.android.heartauth.R
import com.samsung.android.heartauth.data.ScreenState
import com.samsung.android.heartauth.ui.screens.MainScreen
import com.samsung.android.heartauth.ui.screens.MeasurementScreen
import com.samsung.android.heartauth.ui.screens.ResultScreen

@Composable
fun RootScaffold(
    viewModel: RootViewModel,
    startDurationMs: Long = 5_000L
) {
    val ctx = LocalContext.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { ev ->
            when (ev) {
                is RootViewModel.UiEvent.ShowToast ->
                    Toast.makeText(ctx, ctx.getString(ev.messageRes), Toast.LENGTH_SHORT).show()
                RootViewModel.UiEvent.KeepScreenOnEnable -> {
                    (ctx as? Activity)?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                RootViewModel.UiEvent.KeepScreenOnDisable -> {
                    (ctx as? Activity)?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
        }
    }

    val statusText = when {
        ui.statusText == "lead_off" -> ctx.getString(R.string.outputWarning)
        ui.screen is ScreenState.WaitingForContact -> ctx.getString(R.string.outputMeasuring)
        ui.screen is ScreenState.Measuring && ui.secondsLeft >= 0 ->
            ctx.getString(R.string.measuring_time_left, ui.secondsLeft)
        else -> ""
    }

    Surface(Modifier.fillMaxSize()) {
        when (val s = ui.screen) {
            is ScreenState.Menu -> MainScreen(
                onMeasure = { viewModel.startFlow(startDurationMs) },
            )

            is ScreenState.WaitingForContact -> MeasurementScreen(
                title = ctx.getString(R.string.waiting_for_contact_title),
                status = statusText,
            )

            is ScreenState.Measuring -> MeasurementScreen(
                title = ctx.getString(R.string.measuring_title, (s.durationMs / 1000)),
                status = statusText,
            )

            is ScreenState.Result -> {
                val resultText = when (s.finishedReason) {
                    EcgMeasurementController.FinishReason.LEAD_OFF ->
                        ctx.getString(R.string.MeasurementEndedLeadOff)
                    EcgMeasurementController.FinishReason.TIMER ->
                        ctx.getString(R.string.MeasurementSuccessful)
                    EcgMeasurementController.FinishReason.CANCELLED ->
                        ctx.getString(R.string.MeasurementCancelled)
                }
                ResultScreen(message = resultText, onBackHome = { viewModel.stop() })
            }
        }
    }
}
