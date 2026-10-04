package dev.minios.ocremote.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import dev.minios.ocremote.domain.model.AppearanceOverride
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class AppearanceRepositoryTest {
    @Test fun serverScopeAndProjectScopeCannotCollide() {
        assertNotEquals(appearanceScopeKey("a", "project", "b:c"), appearanceScopeKey("a:b", "project", "c"))
        assertNotEquals(appearanceScopeKey("a", "session", "id"), appearanceScopeKey("a", "project", "id"))
    }
    @Test fun reloadClearAndFallbackPreserveOtherScopesAndPreferences() = runBlocking {
        val unrelated = booleanPreferencesKey("notifications_enabled")
        val store = Store(mutablePreferencesOf(unrelated to true))
        val repository = AppearanceRepository(store, Json)
        repository.saveProject("first", "", "/repo/sub/../", AppearanceOverride(accent = "green"))
        repository.saveSession("first", "session", AppearanceOverride(theme = "dark"))
        repository.saveSession("second", "session", AppearanceOverride(accent = "pink"))
        val reloaded = AppearanceRepository(store, Json)
        assertEquals("green", reloaded.projectAppearance("first", "directory:/repo", "/repo//").first()?.accent)
        repository.saveSession("first", "session", null)
        assertNull(reloaded.sessionAppearance("first", "session").first())
        assertEquals("pink", reloaded.sessionAppearance("second", "session").first()?.accent)
        assertTrue(store.data.first()[unrelated] == true)
        repository.saveProject("first", "", "/repo", AppearanceOverride(theme = "invalid"))
        assertNull(reloaded.projectAppearance("first", "", "/repo").first())
    }
    @Test fun missingProjectIdUsesLongestParentDirectoryWithinItsServer() = runBlocking {
        val store = Store(mutablePreferencesOf())
        val repository = AppearanceRepository(store, Json)
        repository.saveProject("first", "outer", "/work/root", AppearanceOverride(accent = "blue"))
        repository.saveProject("first", "inner", "/work/root/sub", AppearanceOverride(accent = "green"))
        repository.saveProject("second", "outer", "/work/root", AppearanceOverride(accent = "pink"))
        val reloaded = AppearanceRepository(store, Json)
        assertEquals("blue", reloaded.projectAppearance("first", "", "/work/root/another").first()?.accent)
        assertEquals("green", reloaded.projectAppearance("first", "", "/work/root/sub/nested").first()?.accent)
        assertEquals("pink", reloaded.projectAppearance("second", "", "/work/root/sub").first()?.accent)
        assertNull(reloaded.projectAppearance("unknown", "", "/work/root/sub").first())
        assertNull(reloaded.projectAppearance("first", "different-project", "/work/root/sub").first())
    }

    @Test fun directoryFallbackRequiresWholePathBoundary() = runBlocking {
        val repository = AppearanceRepository(Store(mutablePreferencesOf()), Json)
        repository.saveProject("server", "project", "/work/foo", AppearanceOverride(theme = "dark"))
        assertEquals("dark", repository.projectAppearance("server", "", "/work/foo/nested").first()?.theme)
        assertNull(repository.projectAppearance("server", "", "/work/foobar").first())
        assertNull(repository.projectAppearance("server", "", "/work").first())
    }

    @Test fun restoringInheritanceClearsAliasesAndMovingProjectRetiresOldPath() = runBlocking {
        val repository = AppearanceRepository(Store(mutablePreferencesOf()), Json)
        repository.saveProject("server", "project", "/old", AppearanceOverride(accent = "teal"))
        repository.saveProject("server", "project", "/new", AppearanceOverride(accent = "red"))
        assertNull(repository.projectAppearance("server", "", "/old/child").first())
        assertEquals("red", repository.projectAppearance("server", "", "/new/child").first()?.accent)
        // Removing a primary clears its aliases even when the caller has a different directory.
        repository.saveProject("server", "project", "/new/child", null)
        assertNull(repository.projectAppearance("server", "project", "/new").first())
        assertNull(repository.projectAppearance("server", "", "/new/child").first())
    }

    private class Store(initial: Preferences) : DataStore<Preferences> {
        private val state = MutableStateFlow(initial)
        override val data: Flow<Preferences> = state
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
            transform(state.value).also { state.value = it }
    }
}
