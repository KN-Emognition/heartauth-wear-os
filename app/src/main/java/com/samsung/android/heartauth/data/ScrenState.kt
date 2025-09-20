package com.samsung.android.heartauth.data

import com.samsung.android.heartauth.EcgMeasurementController

sealed class ScreenState {
    object Menu : ScreenState()
    data class WaitingForContact(val durationMs: Long) : ScreenState()
    data class Measuring(val durationMs: Long) : ScreenState()
    data class Result(
        val success: Boolean,
        val samples: List<EcgMeasurementController.Sample>,
        val finishedReason: EcgMeasurementController.FinishReason
    ) : ScreenState()
}