package dev.minios.ocremote.domain.model

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SessionControlsTest {
    private val controls = SessionControls(autoCompact = true)
    private val completed = Message.Assistant(
        id = "assistant", sessionId = "session", time = TimeInfo(100, 200), parentId = "user",
        providerId = "provider", modelId = "model", finish = "stop",
        tokens = Message.Assistant.Tokens(input = 650, output = 100, cache = Message.Assistant.Tokens.Cache(read = 100)),
    )

    @Test
    fun `controls start opt out and normalize threshold`() {
        assertEquals(SessionControls(false, false, false, 85), SessionControls())
        assertEquals(70, controls.copy(compactionThresholdPercent = 1).normalized().compactionThresholdPercent)
        assertEquals(95, controls.copy(compactionThresholdPercent = 100).normalized().compactionThresholdPercent)
    }

    @Test
    fun `compaction includes cache and triggers exactly at configured percentage`() {
        assertEquals(completed, candidate())
        assertNull(candidate(message = completed.copy(tokens = completed.tokens!!.copy(input = 649))))
        assertEquals(completed, automaticCompactionCandidate(controls.copy(compactionThresholdPercent = 70), SessionStatus.Idle,
            listOf(completed), 1000, null))
        assertNull(automaticCompactionCandidate(controls.copy(compactionThresholdPercent = 95), SessionStatus.Idle,
            listOf(completed), 1000, null))
    }

    @Test
    fun `unknown context disabled busy retry incomplete error and summary are never compacted`() {
        assertNull(candidate(limit = 0))
        assertNull(automaticCompactionCandidate(SessionControls(), SessionStatus.Idle, listOf(completed), 1000, null))
        assertNull(candidate(status = SessionStatus.Busy))
        assertNull(candidate(status = SessionStatus.Retry(1, "retry", 300)))
        assertNull(candidate(status = null))
        assertNull(candidate(message = completed.copy(time = TimeInfo(100))))
        assertNull(candidate(message = completed.copy(error = Message.Assistant.ErrorInfo("error"))))
        assertNull(candidate(message = completed.copy(summary = true)))
        assertNull(candidate(message = completed.copy(finish = "tool-calls")))
        assertNull(candidate(message = completed.copy(providerId = null)))
        assertNull(candidate(message = completed.copy(modelId = null)))
        assertNull(candidate(message = completed.copy(tokens = null)))
    }

    @Test
    fun `an attempted response and a newer prompt cannot cause repeated compaction`() {
        assertNull(automaticCompactionCandidate(controls, SessionStatus.Idle, listOf(completed), 1000, completed.id))
        val prompt = Message.User("new-user", "session", time = TimeInfo(300))
        assertNull(automaticCompactionCandidate(controls, SessionStatus.Idle, listOf(completed, prompt), 1000, null))
        val newer = completed.copy(id = "new-assistant", time = TimeInfo(400, 500))
        assertEquals(newer, automaticCompactionCandidate(controls, SessionStatus.Idle, listOf(completed, prompt, newer), 1000, completed.id))
    }

    @Test
    fun `permission attempt guard deduplicates reconnect snapshots and concurrent SSE`() = runBlocking {
        val attempts = AutomaticPermissionAttempts()
        val key = SessionControlKey("server", "session")
        assertEquals(1, (1..40).map { async { attempts.claim(key, "request") } }.awaitAll().count { it })
        assertFalse(attempts.claim(key, "request"))
        assertTrue(attempts.claim(SessionControlKey("another-server", "session"), "request"))
        assertTrue(attempts.claim(SessionControlKey("server", "child"), "request"))
        assertTrue(attempts.claim(key, "another-request"))
    }

    @Test
    fun `automatic actions require exact and unambiguous server session ownership`() {
        val key = SessionControlKey("server", "session")
        assertTrue(ownsSessionUnambiguously(mapOf("server" to setOf("session", "child")), key))
        assertFalse(ownsSessionUnambiguously(mapOf("another-server" to setOf("session")), key))
        assertFalse(ownsSessionUnambiguously(mapOf("server" to setOf("session"), "another-server" to setOf("session")), key))
        assertFalse(ownsSessionUnambiguously(emptyMap(), key))
    }

    @Test
    fun `automatic permission policy replies once and never broadens approval on reconnect`() = runBlocking {
        val attempts = AutomaticPermissionAttempts()
        val key = SessionControlKey("server", "session")
        val ownership = mapOf("server" to setOf("session", "child"))
        val enabled = SessionControls(autoApprovePermissions = true)
        val replies = mutableListOf<Pair<String, String>>()
        val reply: suspend (String, String) -> Boolean = { id, choice -> replies += id to choice; true }
        assertNull(attempts.replyIfEnabled(key, "request", SessionControls(), ownership, reply))
        assertNull(attempts.replyIfEnabled(SessionControlKey("alias", "session"), "request", enabled, ownership, reply))
        assertEquals(true, attempts.replyIfEnabled(key, "request", enabled, ownership, reply))
        assertNull(attempts.replyIfEnabled(key, "request", enabled, ownership, reply))
        assertEquals(listOf("request" to "once"), replies)
        assertEquals(false, attempts.replyIfEnabled(key, "failed-request", enabled, ownership) { _, _ -> false })
        assertNull(attempts.replyIfEnabled(key, "failed-request", enabled, ownership, reply))
    }

    private fun candidate(message: Message.Assistant = completed, status: SessionStatus? = SessionStatus.Idle, limit: Int = 1000) =
        automaticCompactionCandidate(controls, status, listOf(message), limit, null)
}
