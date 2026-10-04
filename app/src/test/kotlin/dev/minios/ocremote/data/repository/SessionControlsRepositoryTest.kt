package dev.minios.ocremote.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import dev.minios.ocremote.domain.model.SessionControlKey
import dev.minios.ocremote.domain.model.SessionControls
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class SessionControlsRepositoryTest {
    @Test
    fun `consent reloads only for exact identity preserving unrelated settings`() = runBlocking {
        val unrelated = booleanPreferencesKey("unrelated")
        val store = MemoryPreferences(mutablePreferencesOf(unrelated to true))
        val repository = SessionControlsRepository(store, Json)
        val enabled = SessionControls(autoApprovePermissions = true, autoCompact = true, muted = true)
        repository.set("server", "session", enabled)
        repository.set("server:session", "other", SessionControls(muted = true))
        val reloaded = SessionControlsRepository(store, Json)
        assertEquals(enabled, reloaded.controls("server", "session").first())
        assertEquals(SessionControls(), reloaded.controls("another-server", "session").first())
        assertEquals(SessionControls(), reloaded.controls("server", "child").first())
        assertEquals(SessionControls(), reloaded.controls("server", "new-session").first())
        assertEquals(SessionControls(), reloaded.controls("server", "session:other").first())
        assertEquals(true, store.data.first()[unrelated])
        repository.set("server", "session", SessionControls())
        assertEquals(SessionControls(), reloaded.controls("server", "session").first())
        assertTrue(reloaded.controls("server:session", "other").first().muted)
    }

    @Test
    fun `malformed consent fails closed`() {
        val repository = SessionControlsRepository(MemoryPreferences(), Json)
        assertEquals(emptyMap<SessionControlKey, SessionControls>(), repository.decode("invalid"))
        assertEquals(emptyMap<SessionControlKey, SessionControls>(), repository.decode(null))
    }

    @Test
    fun `last compaction attempt survives restart without modifying consent`() = runBlocking {
        val store = MemoryPreferences()
        val repository = SessionControlsRepository(store, Json)
        val key = SessionControlKey("server", "session")
        repository.set(key.serverId, key.sessionId, SessionControls(autoCompact = true))
        repository.markCompactionAttempt(key, "assistant")
        repository.markCompactionAttempt(SessionControlKey("other-server", "session"), "other-assistant")
        val reloaded = SessionControlsRepository(store, Json)
        assertEquals("assistant", reloaded.lastCompactionAttempt(key))
        assertNull(reloaded.lastCompactionAttempt(SessionControlKey("server", "child")))
        assertEquals(SessionControls(autoCompact = true), reloaded.controls(key.serverId, key.sessionId).first())
        repository.markCompactionAttempt(key, "new-assistant")
        assertTrue(reloaded.hasCompactionAttempt(key, "assistant"))
        assertTrue(reloaded.hasCompactionAttempt(key, "new-assistant"))
        assertFalse(reloaded.hasCompactionAttempt(SessionControlKey("server", "child"), "assistant"))
    }

    @Test(expected = java.io.IOException::class)
    fun `failed consent write is explicit rather than pretending it saved`() = runBlocking {
        val store = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = MutableStateFlow(emptyPreferences())
            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
                throw java.io.IOException("fixture storage failure")
            }
        }
        SessionControlsRepository(store, Json).set("server", "session", SessionControls(autoApprovePermissions = true))
    }

    private class MemoryPreferences(initial: Preferences = emptyPreferences()) : DataStore<Preferences> {
        private val state = MutableStateFlow(initial)
        override val data: Flow<Preferences> = state
        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            transform(state.value).also { state.value = it }
    }
}
