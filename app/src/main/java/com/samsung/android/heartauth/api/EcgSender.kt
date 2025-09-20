package com.samsung.android.heartauth.api

interface EcgSender {
    fun sendEcg(payload: EcgDto)
}