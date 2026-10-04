package dev.minios.ocremote.service

import org.junit.Assert.*
import org.junit.Test

class SessionMuteTest {
    @Test
    fun `enabling mute clears exact session alerts once and blocks every later event`() {
        SessionNotificationCoordinator.updateMutedSessions(emptySet()) { _, _ -> }
        val cleared = mutableListOf<Pair<String, String>>()
        val muted = setOf("server" to "session")
        SessionNotificationCoordinator.updateMutedSessions(muted) { server, session -> cleared += server to session }
        SessionNotificationCoordinator.updateMutedSessions(muted) { server, session -> cleared += server to session }
        assertEquals(listOf("server" to "session"), cleared)
        var posted = 0
        sessionEventNotificationIds("server", "session").forEach {
            assertFalse(SessionNotificationCoordinator.postUnlessActive("server", "session") { posted++ })
        }
        assertEquals(0, posted)
        assertTrue(SessionNotificationCoordinator.postUnlessActive("server", "child") { posted++ })
        assertTrue(SessionNotificationCoordinator.postUnlessActive("other-server", "session") { posted++ })
        SessionNotificationCoordinator.updateMutedSessions(emptySet()) { _, _ -> }
        assertTrue(SessionNotificationCoordinator.postUnlessActive("server", "session") { posted++ })
        assertEquals(3, posted)
    }
}
