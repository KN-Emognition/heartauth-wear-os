package com.knemognition.heartauth.ui

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
import com.knemognition.heartauth.core.RootViewModel
import com.knemognition.heartauth.data.FinishReason
import com.knemognition.heartauth.data.ScreenState
import com.knemognition.heartauth.data.UiEvent
import com.knemognition.heartauth.ui.screens.MainScreen
import com.knemognition.heartauth.ui.screens.MeasurementScreen
import com.knemognition.heartauth.ui.screens.ResultScreen
import com.knemognition.heartauth.ui.screens.WaitingScreen
import kotlinx.coroutines.delay

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

                UiEvent.KeepScreenOnEnable ->
                    (ctx as? Activity)?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                UiEvent.KeepScreenOnDisable ->
                    (ctx as? Activity)?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                is UiEvent.CloseAfterDelay -> {
                    delay(ev.ms)
                    (ctx as? Activity)?.finish()
                }
            }
        }
    }



    Surface(Modifier.fillMaxSize()) {
        when (val s = ui.screen) {
            ScreenState.Menu -> MainScreen(onMeasure = { viewModel.startFlow() })
            ScreenState.WaitingForContact -> MeasurementScreen(0f)
            is ScreenState.Measuring -> MeasurementScreen(s.progress)
            is ScreenState.Result -> {
                if (s.success) WaitingScreen()
                else ResultScreen(
                    message = when (s.finishedReason) {
                        FinishReason.LEAD_OFF -> ctx.getString(R.string.result_interrupt)
                        FinishReason.SUCCESS -> ctx.getString(R.string.result_success)
                        FinishReason.TIMEOUT -> ctx.getString(R.string.result_cancel)
                    },
                    onBackHome = { viewModel.goToMenu() })
            }
        }

    }
}
