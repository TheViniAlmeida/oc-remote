package dev.minios.ocremote.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import dev.minios.ocremote.domain.model.NotificationRule
import dev.minios.ocremote.domain.model.NotificationTiming

class SessionNotificationCoordinatorTest {
    @Test
    fun `session event ids retain every existing notification offset`() {
        val baseId = eventNotificationId("server", "session", 0)

        assertEquals(
            listOf(baseId, baseId + 1000, baseId + 2000, baseId + 3000),
            sessionEventNotificationIds("server", "session"),
        )
    }

    @Test
    fun `session event ids differ between sessions`() {
        assertNotEquals(
            sessionEventNotificationIds("server", "first"),
            sessionEventNotificationIds("server", "second"),
        )
    }

    @Test
    fun `notification is suppressed only for the foreground session`() {
        val activeSession = "server" to "active"

        assertFalse(shouldPostSessionNotification(activeSession, "server", "active"))
        assertTrue(shouldPostSessionNotification(activeSession, "server", "other"))
        assertTrue(shouldPostSessionNotification(activeSession, "other-server", "active"))
        assertTrue(shouldPostSessionNotification(null, "server", "active"))
    }

    @Test
    fun `preference delivery applies mute to exact identity and handles server errors`() {
        SessionNotificationCoordinator.updateMutedSessions(setOf("server" to "muted")) { _, _ -> }
        var delivered = 0
        val always = NotificationRule(timing = NotificationTiming.ALWAYS)
        assertFalse(SessionNotificationCoordinator.postWithPreferences("server", "muted", always, true) { delivered++ })
        assertTrue(SessionNotificationCoordinator.postWithPreferences("other", "muted", always, true) { delivered++ })
        assertTrue(SessionNotificationCoordinator.postWithPreferences("server", null, always, true) { delivered++ })
        assertFalse(SessionNotificationCoordinator.postWithPreferences("server", null, NotificationRule(enabled = false), false) { delivered++ })
        assertEquals(2, delivered)
        SessionNotificationCoordinator.updateMutedSessions(emptySet()) { _, _ -> }
    }
}
