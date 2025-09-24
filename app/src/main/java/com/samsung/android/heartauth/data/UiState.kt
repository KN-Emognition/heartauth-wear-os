package com.samsung.android.heartauth.data

import knemognition.heartauth.mobile.data.ScreenState

data class UiState(
    val screen: ScreenState = ScreenState.Menu,
    val progress: Float = 0f,
    val statusText: String = "",
)