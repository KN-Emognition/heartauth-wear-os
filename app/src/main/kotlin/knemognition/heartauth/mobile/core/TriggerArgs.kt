package knemognition.heartauth.mobile.core

import android.os.Bundle
import knemognition.heartauth.mobile.api.TriggerRequest

data class TriggerArgs(val nodeId: String, val req: TriggerRequest) {

    fun toBundle() = Bundle().apply {
        putString("nodeId", nodeId)
        putParcelable("req", req)
    }

    companion object {
        fun from(bundle: Bundle?): TriggerArgs? = bundle?.let {
            val nodeId = it.getString("nodeId") ?: return null
            val req = it.getParcelable<TriggerRequest>("req") ?: return null
            TriggerArgs(nodeId, req)
        }
    }
}