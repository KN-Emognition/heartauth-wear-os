package com.knemognition.heartauth.data

sealed interface UiEvent {
    data class ShowToast(val messageRes: Int) : UiEvent
    data class CloseAfterDelay(val ms: Long) : UiEvent
    data object KeepScreenOnEnable : UiEvent
    data object KeepScreenOnDisable : UiEvent
}