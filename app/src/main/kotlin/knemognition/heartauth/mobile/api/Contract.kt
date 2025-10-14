package knemognition.heartauth.mobile.api

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import org.json.JSONObject

const val AUTH_TRIGGER_PATH = "/heartauth/v1/auth/trigger"
const val AUTH_RESULT_PATH = "/heartauth/v1/auth/result"
const val HEALTH_CHECK_TRIGGER_PATH = "/heartauth/v1/health/trigger"
const val HEALTH_CHECK_RESULT_PATH = "/heartauth/v1/health/result"


const val TYPE_CHALLENGE = "challenge"
const val TYPE_RESULT = "result"

@Parcelize
data class TriggerRequest(
    val id: String,
    val expiresAt: Long,
    val measurementDurationMs: Long,
) : Parcelable

data class TriggerResponse(
    val id: String,
    val ok: Boolean,
    val data: List<Float> = emptyList()
)

fun ByteArray.parseTriggerOrNull(): TriggerRequest? {
    return try {
        val o = JSONObject(String(this))
        if (o.optString("type") != TYPE_CHALLENGE) return null
        TriggerRequest(
            id = o.getString("id"),
            expiresAt = o.getLong("expiresAt"),
            measurementDurationMs = o.getLong("measurementDurationMs"),
        )
    } catch (_: Throwable) {
        null
    }
}

fun TriggerResponse.toJsonBytes(): ByteArray = JSONObject(
    buildMap {
        put("type", TYPE_RESULT)
        put("id", id)
        put("ok", ok)
        if (ok) put("data", JSONArray(data))
    }
).toString().encodeToByteArray()
