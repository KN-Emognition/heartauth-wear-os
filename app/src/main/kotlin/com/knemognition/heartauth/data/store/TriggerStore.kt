package com.knemognition.heartauth.data.store


import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.knemognition.heartauth.core.TriggerArgs
import com.knemognition.heartauth.api.TriggerRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.triggerDataStore: DataStore<Preferences> by preferencesDataStore("trigger_store")

object TriggerStore {
    private val K_NODE = stringPreferencesKey("nodeId")
    private val K_ID = stringPreferencesKey("id")
    private val K_EXP = longPreferencesKey("expiresAt")
    private val K_DUR = longPreferencesKey("measurementDurationMs")

    suspend fun save(ctx: Context, args: TriggerArgs) {
        ctx.triggerDataStore.edit {
            it[K_NODE] = args.nodeId
            it[K_ID] = args.req.id
            it[K_EXP] = args.req.expiresAt
            it[K_DUR] = args.req.measurementDurationMs
        }
    }

    suspend fun load(ctx: Context): SavedTrigger? =
        ctx.triggerDataStore.data.map { p ->
            val node = p[K_NODE] ?: return@map null
            val id = p[K_ID] ?: return@map null
            val exp = p[K_EXP] ?: return@map null
            val dur = p[K_DUR] ?: return@map null
            val args = TriggerArgs(node, TriggerRequest(id, exp, dur))
            SavedTrigger(args)
        }.first()

    suspend fun clear(ctx: Context) {
        ctx.triggerDataStore.edit { it.clear() }
    }

    data class SavedTrigger(val args: TriggerArgs) {
        fun isExpired(now: Long = System.currentTimeMillis()): Boolean =
            now > args.req.expiresAt
    }
}
