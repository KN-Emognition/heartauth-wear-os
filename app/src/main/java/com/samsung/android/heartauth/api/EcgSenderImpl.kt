package com.samsung.android.heartauth.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.samsung.android.heartauth.Constants
import com.samsung.android.heartauth.data.models.EcgPayload

class EcgSenderImpl : EcgSender {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    override fun sendEcg(payload: EcgPayload) {
        val json = gson.toJson(payload)
        Log.i(Constants.HAUTH_TAG, json)
    }
}