package com.knemognition.heartauth.core

import com.knemognition.heartauth.data.FinishReason


interface Listener {
    fun onData()
    fun onStableTick()
    fun onProgress(fraction: Float)
    fun onFinished(success: Boolean, samples: List<Float>, finishedReason: FinishReason)
}