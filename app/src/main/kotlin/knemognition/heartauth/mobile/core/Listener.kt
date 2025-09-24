package knemognition.heartauth.mobile.core

import knemognition.heartauth.mobile.data.FinishReason


interface Listener {
    fun onData()
    fun onStableTick()
    fun onProgress(fraction: Float)
    fun onFinished(success: Boolean, samples: List<Float>, finishedReason: FinishReason)
}