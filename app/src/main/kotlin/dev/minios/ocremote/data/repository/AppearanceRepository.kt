package dev.minios.ocremote.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.minios.ocremote.domain.model.AppearanceOverride
import dev.minios.ocremote.domain.model.projectAppearanceId
import dev.minios.ocremote.domain.model.normalizeProjectDirectory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

internal fun appearanceScopeKey(serverId: String, scope: String, id: String): String =
    "${serverId.length}:$serverId:$scope:$id"

/** Device-local presentation only; separate from project tags, consent and sync settings. */
@Singleton
class AppearanceRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) {
    private val key = stringPreferencesKey("scoped_theme_appearance")
    private val projectDirectoriesKey = stringPreferencesKey("scoped_theme_project_directories")

    fun projectAppearance(serverId: String, projectId: String, directory: String): Flow<AppearanceOverride?> =
        dataStore.data.map { preferences ->
            val entries = decode(preferences[key])
            val primaryId = projectAppearanceId(projectId, directory)
            if (!primaryId.startsWith("directory:")) {
                // A known project ID must never pick up a different project merely sharing its path.
                entries[appearanceScopeKey(serverId, "project", primaryId)]?.usable()
            } else {
                resolveDirectoryAppearance(serverId, directory, entries, decodeDirectories(preferences[projectDirectoriesKey]))
            }
        }

    fun sessionAppearance(serverId: String, sessionId: String): Flow<AppearanceOverride?> =
        appearance(appearanceScopeKey(serverId, "session", sessionId))

    suspend fun saveProject(serverId: String, projectId: String, directory: String, value: AppearanceOverride?) {
        val primaryId = projectAppearanceId(projectId, directory)
        val primary = appearanceScopeKey(serverId, "project", primaryId)
        val normalized = value?.usable()
        dataStore.edit { preferences ->
            val entries = decode(preferences[key]).toMutableMap()
            val directories = decodeDirectories(preferences[projectDirectoriesKey]).toMutableMap()
            if (normalized == null) entries.remove(primary) else entries[primary] = normalized
            // Remove aliases on inheritance restore and when a project moves to a new directory.
            directories.entries.removeAll { it.value == primary }
            if (normalized != null && !primaryId.startsWith("directory:")) {
                directories[appearanceScopeKey(serverId, "project", "directory:${normalizeProjectDirectory(directory)}")] = primary
            }
            preferences[key] = json.encodeToString(entries)
            preferences[projectDirectoriesKey] = json.encodeToString(directories)
        }
    }

    suspend fun saveSession(serverId: String, sessionId: String, value: AppearanceOverride?) =
        save(appearanceScopeKey(serverId, "session", sessionId), value)

    private fun appearance(scope: String): Flow<AppearanceOverride?> = dataStore.data.map {
        decode(it[key])[scope]?.normalized()?.takeUnless(AppearanceOverride::isEmpty)
    }

    private suspend fun save(scope: String, value: AppearanceOverride?) {
        val normalized = value?.normalized()?.takeUnless(AppearanceOverride::isEmpty)
        dataStore.edit { preferences ->
            val entries = decode(preferences[key]).toMutableMap()
            if (normalized == null) entries.remove(scope) else entries[scope] = normalized
            preferences[key] = json.encodeToString(entries)
        }
    }

    private fun resolveDirectoryAppearance(
        serverId: String,
        directory: String,
        entries: Map<String, AppearanceOverride>,
        aliases: Map<String, String>,
    ): AppearanceOverride? {
        val projectPrefix = appearanceScopeKey(serverId, "project", "")
        val directoryPrefix = projectPrefix + "directory:"
        val target = normalizeProjectDirectory(directory)
        val candidates = entries.keys.filter { it.startsWith(directoryPrefix) } +
            aliases.keys.filter { it.startsWith(directoryPrefix) }
        return candidates.distinct().mapNotNull { candidate ->
            val path = candidate.removePrefix(directoryPrefix)
            // A slash boundary prevents /work/foo from matching /work/foobar.
            val matches = path == target || path == "/" || target.startsWith("$path/")
            if (!matches) return@mapNotNull null
            val alias = aliases[candidate]?.takeIf { it.startsWith(projectPrefix) }
            val appearance = entries[candidate]?.usable() ?: alias?.let { entries[it]?.usable() }
            appearance?.let { path to it }
        }.maxByOrNull { it.first.length }?.second
    }

    private fun AppearanceOverride.usable(): AppearanceOverride? = normalized().takeUnless(AppearanceOverride::isEmpty)

    private fun decodeDirectories(encoded: String?): Map<String, String> =
        if (encoded.isNullOrBlank()) emptyMap() else json.decodeFromString(encoded)

    private fun decode(encoded: String?): Map<String, AppearanceOverride> =
        if (encoded.isNullOrBlank()) emptyMap() else json.decodeFromString(encoded)
}
