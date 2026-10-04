package dev.minios.ocremote.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import dev.minios.ocremote.domain.model.ProjectAppearance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectAppearanceRepositoryTest {
    @Test
    fun scopesCannotCollideEvenWhenIdsContainSeparators() {
        assertNotEquals(
            projectAppearanceStorageKey("one", "two:three", "/repo"),
            projectAppearanceStorageKey("one:two", "three", "/repo"),
        )
        assertEquals(
            projectAppearanceStorageKey("server", "", "/repo//"),
            projectAppearanceStorageKey("server", "directory:/repo", "/repo/./"),
        )
    }

    @Test
    fun saveReloadAndClearPreserveOtherServersProjectsAndPreferences() = runBlocking {
        val dynamicColorKey = booleanPreferencesKey("dynamic_color")
        val store = MemoryPreferences(mutablePreferencesOf(dynamicColorKey to true))
        val repository = ProjectAppearanceRepository(store, Json)
        val work = ProjectAppearance("blue", listOf(" Work ", "work"))
        repository.save("first", "project", "/repo", work)
        repository.save("second", "project", "/repo", ProjectAppearance("pink"))
        repository.save("first", "another", "/other", ProjectAppearance("green"))

        val reloaded = ProjectAppearanceRepository(store, Json)
        assertEquals(work.normalized(), reloaded.appearances("first").first()["project"])
        assertEquals("pink", reloaded.appearances("second").first()["project"]?.color)

        repository.save("first", "project", "/moved", ProjectAppearance())
        assertEquals(setOf("another"), reloaded.appearances("first").first().keys)
        assertEquals("pink", reloaded.appearances("second").first()["project"]?.color)
        assertTrue(store.data.first()[dynamicColorKey] == true)
    }

    @Test
    fun directoryFallbackReloadsUsingEquivalentNormalizedPath() = runBlocking {
        val store = MemoryPreferences()
        val repository = ProjectAppearanceRepository(store, Json)
        val appearance = ProjectAppearance("amber", listOf("local"))
        repository.save("server", "", "/repo/sub/../", appearance)
        assertEquals(appearance, repository.appearances("server").first()["directory:/repo"])
    }

    private class MemoryPreferences(initial: Preferences = emptyPreferences()) : DataStore<Preferences> {
        private val state = MutableStateFlow(initial)
        override val data: Flow<Preferences> = state
        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            transform(state.value).also { state.value = it }
    }
}
