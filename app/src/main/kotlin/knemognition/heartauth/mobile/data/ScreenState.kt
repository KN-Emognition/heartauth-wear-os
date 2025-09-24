package knemognition.heartauth.mobile.data

sealed class ScreenState {
    data object Menu : ScreenState()
    data object WaitingForContact : ScreenState()
    data class Measuring(val progress: Float) : ScreenState()
    data class Result(val success: Boolean, val finishedReason: FinishReason) : ScreenState()
}