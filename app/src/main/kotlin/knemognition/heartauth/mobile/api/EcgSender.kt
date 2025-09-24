package knemognition.heartauth.mobile.api

import knemognition.heartauth.mobile.data.EcgData

interface EcgSender {
    fun sendEcg(payload: EcgData)
}