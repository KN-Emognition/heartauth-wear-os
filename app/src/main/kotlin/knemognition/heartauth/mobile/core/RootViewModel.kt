package knemognition.heartauth.mobile.core

import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samsung.android.heartauth.data.UiState
import knemognition.heartauth.mobile.Constants
import knemognition.heartauth.mobile.data.EcgData
import knemognition.heartauth.mobile.api.EcgSender
import knemognition.heartauth.mobile.data.FinishReason
import knemognition.heartauth.mobile.data.ScreenState
import knemognition.heartauth.mobile.data.UiEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RootViewModel(
    private val ecgController: EcgMeasurementController,
    private val ecgSender: EcgSender
) : ViewModel() {


    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()


    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var isMeasuring = false
    private var graceJob: Job? = null

    fun startFlow() {
        viewModelScope.launch {
            _ui.update {
                it.copy(
                    screen = ScreenState.WaitingForContact,
                    statusText = "",
                )
            }
            _events.send(UiEvent.KeepScreenOnEnable)

            ecgController.start(object : Listener {
                override fun onData() {
                    val s = _ui.value.screen
                    if (s is ScreenState.WaitingForContact && !ecgController.isLeadOff()) {
                        _ui.update { it.copy(screen = ScreenState.Measuring(0f)) }
                        isMeasuring = true
                    }
                }

                override fun onProgress(fraction: Float) {
                    Log.i(Constants.HAUTH_TAG, fraction.toString())
                    _ui.update { state ->
                        if (state.screen is ScreenState.Measuring) {
                            state.copy(
                                screen = ScreenState.Measuring(fraction),
                                progress = fraction
                            )
                        } else state
                    }
                }

                override fun onStableTick() {
                    resetGraceTimer()
                }

                override fun onFinished(
                    success: Boolean,
                    samples: List<Float>,
                    finishedReason: FinishReason
                ) {
                    isMeasuring = false
                    _events.trySend(UiEvent.KeepScreenOnDisable)

                    if (finishedReason == FinishReason.SUCCESS) {
                        ecgSender.sendEcg(EcgData(samples))
                        _events.trySend(UiEvent.ShowToast(com.samsung.android.heartauth.R.string.result_success))
                        _events.trySend(UiEvent.CloseAfterDelay(2000))
                    }

                    _ui.update {
                        it.copy(
                            screen = ScreenState.Result(
                                success = success,
                                finishedReason = finishedReason
                            )
                        )
                    }
                }

            })

        }
    }

    fun stop() {
        ecgController.stop()
        viewModelScope.launch { _events.send(UiEvent.KeepScreenOnDisable) }
        _ui.update { UiState(screen = ScreenState.Menu) }
    }

    private fun resetGraceTimer() {
        if (_ui.value.screen !is ScreenState.WaitingForContact) return

        graceJob?.cancel()
        graceJob = viewModelScope.launch {
            val deadline = SystemClock.elapsedRealtime() + Constants.ECG_LID_GRACE_PERIOD
            while (isActive && SystemClock.elapsedRealtime() < deadline) {
                if (_ui.value.screen !is ScreenState.WaitingForContact) return@launch
                delay(100)
            }
            if (_ui.value.screen is ScreenState.WaitingForContact) {
                stop()
                _events.send(UiEvent.ShowToast(com.samsung.android.heartauth.R.string.result_cancel))
            }
        }
    }

    fun goToMenu() {
        _ui.update { it.copy(screen = ScreenState.Menu) }
    }

    override fun onCleared() {
        super.onCleared()
        ecgController.stop()
    }
}
