package com.nothatcher.sproutbook

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class SproutRepository(context: Context) {
    private val prefs = context.getSharedPreferences("sproutbook_v300", Context.MODE_PRIVATE)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    fun load(): SproutState {
        val raw = prefs.getString("state", null) ?: return SproutState().normalized()
        return runCatching { json.decodeFromString<SproutState>(raw).normalized() }
            .getOrElse { SproutState().normalized() }
    }

    fun save(state: SproutState) {
        prefs.edit().putString("state", json.encodeToString(state.normalized())).apply()
    }

    fun exportJson(state: SproutState): String = json.encodeToString(state.normalized())

    fun importJson(raw: String): SproutState? =
        runCatching { json.decodeFromString<SproutState>(raw).normalized() }.getOrNull()
}
