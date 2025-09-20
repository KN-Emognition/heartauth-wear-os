package com.samsung.android.heartauth.data

sealed interface UiEvent {
    data class ShowToast(val messageRes: Int) : UiEvent
    data object KeepScreenOnEnable : UiEvent
    data object KeepScreenOnDisable : UiEvent
}