package dev.minios.ocremote.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import dev.minios.ocremote.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class TimelineRepositoryTest {
    @Test fun timelineKeysCannotCollideAcrossServerOrSessionScopes() {
        assertNotEquals(timelineScopeKey("first", "second:third"), timelineScopeKey("first:second", "third"))
        assertNotEquals(timelineScopeKey("first", null), timelineScopeKey("first", "server"))
    }
    @Test fun persistedOverridesRemainLocalAndRestoreInheritanceOnClear() = runBlocking {
        val unrelated = booleanPreferencesKey("auto_scroll_enabled")
        val store = Store(mutablePreferencesOf(unrelated to false))
        val repo = TimelineRepository(store, Json)
        val compact = TimelinePreferences.forPreset(TimelinePreset.COMPACT)
        val detailed = TimelinePreferences.forPreset(TimelinePreset.DETAILED)
        repo.setServer("first", compact)
        repo.setSession("first", "session", detailed)
        repo.setServer("second", TimelinePreferences.forPreset(TimelinePreset.MINIMAL))
        val reload = TimelineRepository(store, Json)
        assertEquals(detailed, reload.resolvedTimeline("first", "session").first())
        assertEquals(compact, reload.resolvedTimeline("first", "other").first())
        repo.setSession("first", "session", null)
        assertNull(reload.sessionOverride("first", "session").first())
        assertEquals(compact, reload.resolvedTimeline("first", "session").first())
        assertEquals(TimelinePreset.MINIMAL, reload.serverTimeline("second").first().preset)
        assertEquals(TimelinePreferences(), reload.serverTimeline("new").first())
        assertFalse(store.data.first()[unrelated]!!)
    }
    private class Store(initial: Preferences) : DataStore<Preferences> {
        private val state = MutableStateFlow(initial)
        override val data: Flow<Preferences> = state
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
            transform(state.value).also { state.value = it }
    }
}
