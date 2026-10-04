package dev.minios.ocremote.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.minios.ocremote.domain.model.ProjectAppearance
import dev.minios.ocremote.domain.model.projectAppearanceId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

internal fun projectAppearanceScopePrefix(serverId: String): String = "${serverId.length}:$serverId:"

internal fun projectAppearanceStorageKey(serverId: String, projectId: String, directory: String): String =
    projectAppearanceScopePrefix(serverId) + projectAppearanceId(projectId, directory)

@Singleton
class ProjectAppearanceRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) {
    private val key = stringPreferencesKey("project_appearances")

    fun appearances(serverId: String): Flow<Map<String, ProjectAppearance>> = dataStore.data.map { preferences ->
        val prefix = projectAppearanceScopePrefix(serverId)
        decode(preferences[key]).filterKeys { it.startsWith(prefix) }
            .mapKeys { it.key.removePrefix(prefix) }.mapValues { it.value.normalized() }
    }

    suspend fun save(serverId: String, projectId: String, directory: String, appearance: ProjectAppearance) {
        val storageKey = projectAppearanceStorageKey(serverId, projectId, directory)
        val normalized = appearance.normalized()
        dataStore.edit { preferences ->
            // Decode strictly when writing so malformed data never silently erases other preferences.
            val entries = decode(preferences[key]).toMutableMap()
            if (normalized.isEmpty) entries.remove(storageKey) else entries[storageKey] = normalized
            preferences[key] = json.encodeToString(entries)
        }
    }

    private fun decode(value: String?): Map<String, ProjectAppearance> =
        if (value.isNullOrBlank()) emptyMap() else json.decodeFromString(value)
}
