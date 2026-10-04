package dev.minios.ocremote.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionHistoryReadinessTest {
    private val session = SessionControlKey("server", "session")

    @Test
    fun `only a successful snapshot authorizes cached history in this connection`() {
        val readiness = SessionHistoryReadiness()
        val connection = Any()
        assertFalse(readiness.isReady(session, connection, "cached-assistant"))
        readiness.markSnapshot(session, connection)
        assertTrue(readiness.isReady(session, connection, "cached-assistant"))
        assertFalse(readiness.isReady(SessionControlKey("server", "other-session"), connection, "cached-assistant"))
        assertFalse(readiness.isReady(SessionControlKey("other-server", "session"), connection, "cached-assistant"))
        readiness.invalidateSession(session, connection)
        assertFalse(readiness.isReady(session, connection, "cached-assistant"))
    }

    @Test
    fun `reconnect invalidates history even when SSE coroutine job is reused`() {
        val readiness = SessionHistoryReadiness()
        val connection = Any()
        readiness.markSnapshot(session, connection)
        readiness.invalidateServer("server")
        // A failed REST history refresh leaves readiness unset despite cached idle/completed data.
        assertFalse(readiness.isReady(session, connection, "cached-assistant"))
        readiness.markCompletedMessage(session, connection, "new-assistant")
        assertTrue(readiness.isReady(session, connection, "new-assistant"))
        assertFalse(readiness.isReady(session, connection, "cached-assistant"))
    }

    @Test
    fun `replacement job cannot use former history and stale failure cannot invalidate new snapshot`() {
        val readiness = SessionHistoryReadiness()
        val former = Any()
        val replacement = Any()
        readiness.markSnapshot(session, former)
        assertFalse(readiness.isReady(session, replacement, "assistant"))
        readiness.markSnapshot(session, replacement)
        readiness.invalidateSession(session, former)
        assertTrue(readiness.isReady(session, replacement, "assistant"))
        assertFalse(readiness.isReady(session, former, "assistant"))
    }

    @Test
    fun `failure or disconnect in one server preserves another server readiness`() {
        val readiness = SessionHistoryReadiness()
        val connection = Any()
        val other = SessionControlKey("other-server", "session")
        readiness.markSnapshot(session, connection)
        readiness.markSnapshot(other, connection)
        readiness.invalidateServer("server")
        assertFalse(readiness.isReady(session, connection))
        assertTrue(readiness.isReady(other, connection))
    }
}
