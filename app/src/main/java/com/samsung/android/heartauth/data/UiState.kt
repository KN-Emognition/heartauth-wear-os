package com.samsung.android.heartauth.data

data class UiState(
    val screen: ScreenState = ScreenState.Menu,
    val progress: Float = 0f,
    val statusText: String = "",
)