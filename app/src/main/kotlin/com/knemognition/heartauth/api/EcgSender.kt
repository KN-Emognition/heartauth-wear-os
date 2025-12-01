package com.knemognition.heartauth.api

import com.knemognition.heartauth.data.EcgData

interface EcgSender {
    fun sendEcg(payload: EcgData)
}