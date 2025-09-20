package com.samsung.android.heartauth.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.heartauth.EcgMeasurementController
import com.samsung.android.heartauth.data.ScreenState
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

    sealed interface UiEvent {
        data class ShowToast(val messageRes: Int) : UiEvent
        data object KeepScreenOnEnable : UiEvent
        data object KeepScreenOnDisable : UiEvent
    }

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val waitingTimeoutMs = 2_000L
    private val tickMs = 100L

    fun startFlow(durationMs: Long) {
        viewModelScope.launch {
            _ui.update {
                it.copy(
                    screen = ScreenState.WaitingForContact(durationMs),
                    statusText = "",
                    secondsLeft = (durationMs / 1000).toInt(),
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
                        runMeasuringLoop()
                    }
                }

                override fun onFinished(
                    success: Boolean,
                    samples: List<EcgMeasurementController.Sample>,
                    finishedReason: EcgMeasurementController.FinishReason
                ) {
                    _events.trySend(UiEvent.KeepScreenOnDisable)
                    _ui.update {
                        it.copy(
                            screen = ScreenState.Result(success, samples, finishedReason)
                        )
                    }
                    Log.i("ESSA PROBKI",samples.size.toString())

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
        val start = System.currentTimeMillis()
        while (isActive && System.currentTimeMillis() - start < waitingTimeoutMs) {
            if (_ui.value.screen !is ScreenState.WaitingForContact) return@launch
            delay(100)
        }
        if (_ui.value.screen is ScreenState.WaitingForContact) {
            stop()
            _events.send(UiEvent.ShowToast(com.samsung.android.heartauth.R.string.outputWarning))
        }
    }

    private fun runMeasuringLoop() = viewModelScope.launch {
        val total = (_ui.value.screen as? ScreenState.Measuring)?.durationMs ?: return@launch

        while (isActive && _ui.value.screen is ScreenState.Measuring) {
            val canAccumulate = ecgController.isArmed() && !ecgController.isLeadOff()
            if (canAccumulate) {
                _ui.update { st ->
                    val new = (st.measuredValidMs + tickMs).coerceAtMost(total)
                    st.copy(
                        measuredValidMs = new,
                        secondsLeft = ((total - new) / 1000).toInt()
                    )
                }
            } else {
                _ui.update { st ->
                    st.copy(
                        secondsLeft = (((total - st.measuredValidMs).coerceAtLeast(0L)) / 1000).toInt()
                    )
                }
            }

            if (_ui.value.measuredValidMs >= total) break
            delay(tickMs)
        }

        if (_ui.value.screen is ScreenState.Measuring) {
            ecgController.finishFromTimer()
        }
    }

    override fun onCleared() {
        super.onCleared()
        ecgController.stop()
    }
}
