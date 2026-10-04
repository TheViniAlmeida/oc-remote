package dev.minios.ocremote.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import dev.minios.ocremote.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class NotificationPreferencesRepositoryTest {
    @Test fun `inheritance is app then exact server then exact session and reset preserves neighbors`() = runBlocking {
        val unrelated = booleanPreferencesKey("fixture_unrelated")
        val store = MemoryStore(mutablePreferencesOf(unrelated to true))
        val repo = NotificationPreferencesRepository(store, Json)
        val app = NotificationPreferences(agent = NotificationRule(enabled = false))
        val server = NotificationPreferences(permission = NotificationRule(sound = NotificationSound.SILENT))
        val session = NotificationPreferences(error = NotificationRule(enabled = false))
        repo.set(NotificationPreferenceScope(), app)
        repo.set(NotificationPreferenceScope("server"), server)
        repo.set(NotificationPreferenceScope("server", "session"), session)
        val reloaded = NotificationPreferencesRepository(store, Json)
        assertEquals(session, reloaded.preferences("server", "session").first())
        assertEquals(server, reloaded.preferences("server", "another").first())
        assertEquals(app, reloaded.preferences("other", "session").first())
        repo.set(NotificationPreferenceScope("server", "session"), null)
        assertEquals(server, reloaded.preferences("server", "session").first())
        assertEquals(true, store.data.first()[unrelated])
        assertEquals(app, reloaded.preferences().first())
    }

    @Test fun `no override uses supplied legacy defaults`() {
        val defaults = legacyNotificationPreferences(false, true)
        assertEquals(defaults, NotificationPreferenceSnapshot().resolve("server", "session", defaults))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `session scope cannot be orphaned from its server`() {
        NotificationPreferenceScope(sessionId = "session")
    }

    @Test(expected = kotlinx.serialization.SerializationException::class)
    fun `corrupt preferences are explicit instead of silently replacing overrides`() {
        NotificationPreferencesRepository(MemoryStore(), Json).decode("invalid")
    }

    private class MemoryStore(initial: Preferences = emptyPreferences()) : DataStore<Preferences> {
        private val state = MutableStateFlow(initial)
        override val data: Flow<Preferences> = state
        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = transform(state.value).also { state.value = it }
    }
}
