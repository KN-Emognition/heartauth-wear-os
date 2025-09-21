package com.samsung.android.heartauth.api

import com.samsung.android.heartauth.data.models.EcgPayload

interface EcgSender {
    fun sendEcg(payload: EcgPayload)
}