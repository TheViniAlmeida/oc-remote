package dev.minios.ocremote.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.minios.ocremote.domain.model.TimelinePreferences
import dev.minios.ocremote.domain.model.resolveTimeline
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

internal fun timelineScopeKey(serverId: String, sessionId: String?): String =
    "${serverId.length}:$serverId:" + (sessionId?.let { "session:$it" } ?: "server")

@Singleton
class TimelineRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) {
    private val key = stringPreferencesKey("timeline_preferences")

    fun serverOverride(serverId: String): Flow<TimelinePreferences?> = entry(timelineScopeKey(serverId, null))

    fun serverTimeline(serverId: String): Flow<TimelinePreferences> = serverOverride(serverId).map {
        it ?: TimelinePreferences()
    }

    fun sessionOverride(serverId: String, sessionId: String): Flow<TimelinePreferences?> =
        entry(timelineScopeKey(serverId, sessionId))

    fun resolvedTimeline(serverId: String, sessionId: String): Flow<TimelinePreferences> =
        combine(serverTimeline(serverId), sessionOverride(serverId, sessionId), ::resolveTimeline)

    suspend fun setServer(serverId: String, value: TimelinePreferences) = save(timelineScopeKey(serverId, null), value)
    suspend fun setSession(serverId: String, sessionId: String, value: TimelinePreferences?) =
        save(timelineScopeKey(serverId, sessionId), value)

    private fun entry(scope: String): Flow<TimelinePreferences?> = dataStore.data.map { decode(it[key])[scope]?.normalized() }

    private suspend fun save(scope: String, value: TimelinePreferences?) {
        dataStore.edit { preferences ->
            val entries = decode(preferences[key]).toMutableMap()
            if (value == null) entries.remove(scope) else entries[scope] = value.normalized()
            preferences[key] = json.encodeToString(entries)
        }
    }

    private fun decode(encoded: String?): Map<String, TimelinePreferences> =
        if (encoded.isNullOrBlank()) emptyMap() else json.decodeFromString(encoded)
}
