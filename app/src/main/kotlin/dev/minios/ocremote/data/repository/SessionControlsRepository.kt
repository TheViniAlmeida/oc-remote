package dev.minios.ocremote.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.minios.ocremote.domain.model.SessionControlKey
import dev.minios.ocremote.domain.model.SessionControls
import dev.minios.ocremote.domain.model.SessionControlFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** Local consent is deliberately absent from sync/export DTOs. */
@Singleton
class SessionControlsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) {
    private val controlsKey = stringPreferencesKey("device_local_session_controls_v1")
    private val attemptsKey = stringPreferencesKey("device_local_compaction_attempts_v1")
    private val _failures = MutableStateFlow<Map<SessionControlKey, SessionControlFailure>>(emptyMap())
    val failures = _failures.asStateFlow()

    fun reportFailure(key: SessionControlKey, failure: SessionControlFailure) {
        _failures.update { it + (key to failure) }
    }

    @Serializable
    internal data class Entry(val key: SessionControlKey, val controls: SessionControls)

    @Serializable
    private data class CompactionAttempt(val key: SessionControlKey, val messageId: String)

    val all: Flow<Map<SessionControlKey, SessionControls>> = dataStore.data.map { preferences ->
        decode(preferences[controlsKey])
    }.distinctUntilChanged()

    fun controls(serverId: String, sessionId: String): Flow<SessionControls> =
        all.map { it[SessionControlKey(serverId, sessionId)] ?: SessionControls() }.distinctUntilChanged()

    suspend fun set(serverId: String, sessionId: String, controls: SessionControls) {
        require(serverId.isNotBlank() && sessionId.isNotBlank())
        val key = SessionControlKey(serverId, sessionId)
        dataStore.edit { preferences ->
            val entries = decode(preferences[controlsKey]).toMutableMap()
            val normalized = controls.normalized()
            if (normalized == SessionControls()) entries.remove(key) else entries[key] = normalized
            preferences[controlsKey] = json.encodeToString(entries.map { Entry(it.key, it.value) })
        }
        _failures.update { it - key }
    }

    suspend fun lastCompactionAttempt(key: SessionControlKey): String? =
        decodeAttempts(dataStore.data.first()[attemptsKey]).lastOrNull { it.key == key }?.messageId

    suspend fun hasCompactionAttempt(key: SessionControlKey, messageId: String): Boolean =
        decodeAttempts(dataStore.data.first()[attemptsKey]).any { it.key == key && it.messageId == messageId }

    suspend fun markCompactionAttempt(key: SessionControlKey, messageId: String) {
        dataStore.edit { preferences ->
            val attempts = decodeAttempts(preferences[attemptsKey]).filterNot { it.key == key && it.messageId == messageId }
            preferences[attemptsKey] = json.encodeToString(attempts + CompactionAttempt(key, messageId))
        }
    }

    internal fun decode(raw: String?): Map<SessionControlKey, SessionControls> {
        if (raw == null) return emptyMap()
        // Malformed consent fails closed. Updating it still reports DataStore write failures to the UI.
        return try {
            json.decodeFromString<List<Entry>>(raw).filter { it.key.serverId.isNotBlank() && it.key.sessionId.isNotBlank() }
                .associate { it.key to it.controls.normalized() }
        } catch (_: IllegalArgumentException) {
            emptyMap()
        }
    }

    private fun decodeAttempts(raw: String?): List<CompactionAttempt> {
        if (raw == null) return emptyList()
        return json.decodeFromString(raw)
    }
}
