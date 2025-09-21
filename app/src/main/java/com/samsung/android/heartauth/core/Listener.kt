package com.samsung.android.heartauth.core

import com.samsung.android.heartauth.data.FinishReason

interface Listener {
    fun onData()
    fun onStableTick()
    fun onProgress(fraction: Float)
    fun onFinished(success: Boolean, samples: List<Float>, finishedReason: FinishReason)
}