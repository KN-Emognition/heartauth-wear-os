package com.samsung.android.heartauth.ui

import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.heartauth.Constants
import com.samsung.android.heartauth.EcgMeasurementController
import com.samsung.android.heartauth.data.ScreenState
import com.samsung.android.heartauth.data.UiEvent
import com.samsung.android.heartauth.data.UiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RootViewModel(
    private val ecgController: EcgMeasurementController
) : ViewModel() {


    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private val MEASUREMENT_DURATION= 2000

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()
    private var isMeasuring = false

    fun startFlow() {
        viewModelScope.launch {
            _ui.update {
                it.copy(
                    screen = ScreenState.WaitingForContact(MEASUREMENT_DURATION.toLong()),
                    statusText = "",
                    secondsLeft = (MEASUREMENT_DURATION / 1000),
                    measuredValidMs = 0L
                )
            }
            _events.send(UiEvent.KeepScreenOnEnable)

            ecgController.start(object : EcgMeasurementController.Listener {
                override fun onLeadOff() {
                    _ui.update { it.copy(statusText = "lead_off") }
                }

                override fun onData() {
                    val s = _ui.value.screen
                    if (s is ScreenState.WaitingForContact && !ecgController.isLeadOff()) {
                        _ui.update { it.copy(screen = ScreenState.Measuring(s.durationMs)) }
                        isMeasuring=true
                    }
                }

                override fun onFinished(
                    success: Boolean,
                    samples: List<EcgMeasurementController.Sample>,
                    finishedReason: EcgMeasurementController.FinishReason
                ) {
                    isMeasuring = false
                    _events.trySend(UiEvent.KeepScreenOnDisable)
                    _ui.update {
                        it.copy(
                            screen = ScreenState.Result(
                                success,
                                samples,
                                finishedReason
                            )
                        )
                    }
                    Log.i("ESSA PROBKA",samples.size.toString())
                }
            })

            runWaitingTimeout()
        }
    }

    fun stop() {
        ecgController.stop()
        viewModelScope.launch { _events.send(UiEvent.KeepScreenOnDisable) }
        _ui.update { UiState(screen = ScreenState.Menu) }
    }

    private fun runWaitingTimeout() = viewModelScope.launch {
        val start = SystemClock.elapsedRealtime()
        while (
            isActive &&
            SystemClock.elapsedRealtime() - start < Constants.ECG_LID_GRACE_PERIOD
        ) {
            if (_ui.value.screen !is ScreenState.WaitingForContact) return@launch
            delay(100)
        }
        if (_ui.value.screen is ScreenState.WaitingForContact) {
            stop()
            _events.send(UiEvent.ShowToast(com.samsung.android.heartauth.R.string.outputWarning))
        }
    }

    override fun onCleared() {
        super.onCleared()
        ecgController.stop()
    }
}
