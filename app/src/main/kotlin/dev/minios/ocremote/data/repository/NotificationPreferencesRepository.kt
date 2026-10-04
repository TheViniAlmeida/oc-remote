package dev.minios.ocremote.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.minios.ocremote.domain.model.NotificationPreferenceScope
import dev.minios.ocremote.domain.model.NotificationPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

data class NotificationPreferenceSnapshot(val overrides: Map<NotificationPreferenceScope, NotificationPreferences> = emptyMap()) {
    fun resolve(serverId: String? = null, sessionId: String? = null, defaults: NotificationPreferences = NotificationPreferences()): NotificationPreferences =
        (if (serverId != null && sessionId != null) overrides[NotificationPreferenceScope(serverId, sessionId)] else null)
            ?: (if (serverId != null) overrides[NotificationPreferenceScope(serverId)] else null)
            ?: overrides[NotificationPreferenceScope()] ?: defaults
}

/** Local device preferences, deliberately excluded from server requests and sync DTOs. */
@Singleton
class NotificationPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) {
    private val key = stringPreferencesKey("device_local_notification_preferences_v1")

    @Serializable
    internal data class Entry(val scope: NotificationPreferenceScope, val preferences: NotificationPreferences)

    val all: Flow<NotificationPreferenceSnapshot> = dataStore.data.map { decode(it[key]) }.distinctUntilChanged()

    fun preferences(serverId: String? = null, sessionId: String? = null): Flow<NotificationPreferences> =
        all.map { it.resolve(serverId, sessionId) }.distinctUntilChanged()

    fun override(scope: NotificationPreferenceScope): Flow<NotificationPreferences?> =
        all.map { it.overrides[scope] }.distinctUntilChanged()

    suspend fun set(scope: NotificationPreferenceScope, preferences: NotificationPreferences?) {
        dataStore.edit { stored ->
            // Do not overwrite a corrupt preferences document with an incomplete map.
            val entries = decode(stored[key]).overrides.toMutableMap()
            if (preferences == null) entries.remove(scope) else entries[scope] = preferences
            stored[key] = json.encodeToString(entries.map { Entry(it.key, it.value) })
        }
    }

    internal fun decode(raw: String?): NotificationPreferenceSnapshot =
        if (raw == null) NotificationPreferenceSnapshot() else
            NotificationPreferenceSnapshot(json.decodeFromString<List<Entry>>(raw).associate { it.scope to it.preferences })
}
