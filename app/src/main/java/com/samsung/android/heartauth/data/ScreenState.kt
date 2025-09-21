package com.samsung.android.heartauth.data

sealed class ScreenState {
    object Menu : ScreenState()
    data class WaitingForContact(val durationMs: Long) : ScreenState()
    data class Measuring(val progress: Float) : ScreenState()
    data class Result(
        val success: Boolean,
        val samples: List<Float>,
        val finishedReason: FinishReason
    ) : ScreenState()
}