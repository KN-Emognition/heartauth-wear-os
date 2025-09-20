package com.samsung.android.heartauth.data

import com.samsung.android.heartauth.core.EcgMeasurementController

sealed class ScreenState {
    object Menu : ScreenState()
    data class WaitingForContact(val durationMs: Long) : ScreenState()
    data class Measuring(val progress: Float) : ScreenState()
    data class Result(
        val success: Boolean,
        val samples: List<Float>,
        val finishedReason: EcgMeasurementController.FinishReason
    ) : ScreenState()
}