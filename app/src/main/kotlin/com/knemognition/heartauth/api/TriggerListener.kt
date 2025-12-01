package com.knemognition.heartauth.api

import android.content.Intent
import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.knemognition.heartauth.Constants
import com.knemognition.heartauth.MeasurementActivity
import com.knemognition.heartauth.core.TriggerArgs
import com.knemognition.heartauth.data.store.TriggerStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TriggerListenerService : WearableListenerService() {

    private val ioScope = CoroutineScope(Dispatchers.IO)

    override fun onMessageReceived(event: MessageEvent) {
        when (event.path) {
            AUTH_TRIGGER_PATH -> {
                val req = event.data.parseTriggerOrNull()
                Log.i(Constants.HAUTH_TAG, "Trigger message received {${req}")
                if (req == null) return
                val args = TriggerArgs(event.sourceNodeId, req)
                ioScope.launch {
                    TriggerStore.save(applicationContext, args)
                }
                val i = Intent(this, MeasurementActivity::class.java)
                    .addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                    .putExtras(args.toBundle())
                startActivity(i)
            }

            HEALTH_CHECK_TRIGGER_PATH -> {
                val response = "OK".toByteArray()
                val nodeId = event.sourceNodeId
                val client = com.google.android.gms.wearable.Wearable.getMessageClient(this)
                client.sendMessage(nodeId, HEALTH_CHECK_RESULT_PATH, response)
            }
        }
    }
}