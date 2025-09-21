package com.samsung.android.heartauth.ui

import android.app.Activity
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samsung.android.heartauth.R
import com.samsung.android.heartauth.data.FinishReason
import com.samsung.android.heartauth.core.RootViewModel
import com.samsung.android.heartauth.data.ScreenState
import com.samsung.android.heartauth.data.UiEvent
import com.samsung.android.heartauth.ui.screens.MainScreen
import com.samsung.android.heartauth.ui.screens.MeasurementScreen
import com.samsung.android.heartauth.ui.screens.ResultScreen

@Composable
fun RootScaffold(
    viewModel: RootViewModel,
) {
    val ctx = LocalContext.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { ev ->
            when (ev) {
                is UiEvent.ShowToast ->
                    Toast.makeText(ctx, ctx.getString(ev.messageRes), Toast.LENGTH_SHORT).show()

                UiEvent.KeepScreenOnEnable -> {
                    (ctx as? Activity)?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }

                UiEvent.KeepScreenOnDisable -> {
                    (ctx as? Activity)?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
        }
    }


    Surface(Modifier.fillMaxSize()) {
        when (val s = ui.screen) {
            is ScreenState.Menu -> MainScreen(
                onMeasure = { viewModel.startFlow() },
            )

            is ScreenState.WaitingForContact -> MeasurementScreen(0f)

            is ScreenState.Measuring -> MeasurementScreen(ui.progress)

            is ScreenState.Result -> {
                val resultText = when (s.finishedReason) {
                    FinishReason.LEAD_OFF ->
                        ctx.getString(R.string.result_interrupt)

                    FinishReason.SUCCESS ->
                        ctx.getString(R.string.result_success)

                    FinishReason.TIMEOUT ->
                        ctx.getString(R.string.result_cancel)
                }
                ResultScreen(message = resultText, onBackHome = { viewModel.stop() })
            }
        }
    }
}
