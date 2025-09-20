package com.samsung.android.heartauth.data

data class UiState(
    val screen: ScreenState = ScreenState.Menu,
    val statusText: String = "",
    val secondsLeft: Int = 0,
    val measuredValidMs: Long = 0L
)