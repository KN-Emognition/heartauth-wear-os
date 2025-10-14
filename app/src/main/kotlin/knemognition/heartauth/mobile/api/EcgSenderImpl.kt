package knemognition.heartauth.mobile.api

import android.content.Context
import com.google.android.gms.wearable.Wearable
import knemognition.heartauth.mobile.data.EcgData
import knemognition.heartauth.mobile.data.store.TriggerStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class EcgSenderImpl(
    private val nodeId: String, context: Context, private val requestId: String
) : EcgSender {

    private val appContext = context.applicationContext
    private val client = Wearable.getMessageClient(appContext)
    private val ioScope = CoroutineScope(Dispatchers.IO)

    override fun sendEcg(payload: EcgData) {
        val response = TriggerResponse(
            requestId, true, payload.data
        )
        client.sendMessage(nodeId, AUTH_RESULT_PATH, response.toJsonBytes())
        ioScope.launch {
            TriggerStore.clear(appContext)
        }
    }
}
