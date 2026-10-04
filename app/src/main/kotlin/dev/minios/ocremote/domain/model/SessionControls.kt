package dev.minios.ocremote.domain.model

import kotlinx.serialization.Serializable

/** Device-local consent for one exact configured server and session. Never inherited. */
@Serializable
data class SessionControls(
    val autoApprovePermissions: Boolean = false,
    val autoCompact: Boolean = false,
    val muted: Boolean = false,
    val compactionThresholdPercent: Int = 85,
) {
    fun normalized(): SessionControls = copy(compactionThresholdPercent = compactionThresholdPercent.coerceIn(70, 95))
}

@Serializable
data class SessionControlKey(val serverId: String, val sessionId: String)

enum class SessionControlFailure { PERMISSION, COMPACTION }

internal fun ownsSessionUnambiguously(
    serverSessions: Map<String, Set<String>>,
    key: SessionControlKey,
): Boolean = key.sessionId in serverSessions[key.serverId].orEmpty() &&
    serverSessions.none { (serverId, ids) -> serverId != key.serverId && key.sessionId in ids }

/** A completed final response, including cached input, is the only safe compaction trigger. */
internal fun automaticCompactionCandidate(
    controls: SessionControls,
    status: SessionStatus?,
    messages: List<Message>,
    contextLimit: Int,
    lastAttemptedMessageId: String?,
): Message.Assistant? {
    if (!controls.autoCompact || status !is SessionStatus.Idle || contextLimit <= 0) return null
    val assistant = messages.maxByOrNull { it.time.created } as? Message.Assistant ?: return null
    if (assistant.time.completed == null || assistant.error != null || assistant.summary == true ||
        assistant.finish == "tool-calls" || assistant.id == lastAttemptedMessageId ||
        assistant.providerId.isNullOrBlank() || assistant.modelId.isNullOrBlank()) return null
    val tokens = assistant.tokens ?: return null
    val used = tokens.input.toLong() + tokens.output + tokens.reasoning + tokens.cache.read + tokens.cache.write
    if (used <= 0 || used * 100 < contextLimit.toLong() * controls.normalized().compactionThresholdPercent) return null
    return assistant
}

/** A failed or successful automatic reply is not replayed by duplicate SSE or REST snapshots. */
internal class AutomaticPermissionAttempts {
    private val attempted = mutableSetOf<Pair<SessionControlKey, String>>()

    @Synchronized
    fun claim(key: SessionControlKey, requestId: String): Boolean = attempted.add(key to requestId)

    suspend fun replyIfEnabled(
        key: SessionControlKey,
        requestId: String,
        controls: SessionControls,
        serverSessions: Map<String, Set<String>>,
        reply: suspend (String, String) -> Boolean,
    ): Boolean? {
        if (!controls.autoApprovePermissions || !ownsSessionUnambiguously(serverSessions, key) || !claim(key, requestId)) return null
        return reply(requestId, "once")
    }
}

/** REST snapshots authorize this connection's history; live completion authorizes only that message. */
internal class SessionHistoryReadiness {
    private data class Source(val connection: Any, val completedMessageId: String?)
    private val sources = mutableMapOf<SessionControlKey, Source>()

    @Synchronized
    fun markSnapshot(key: SessionControlKey, connection: Any) {
        sources[key] = Source(connection, null)
    }

    @Synchronized
    fun markCompletedMessage(key: SessionControlKey, connection: Any, messageId: String) {
        sources[key] = Source(connection, messageId)
    }

    @Synchronized
    fun isReady(key: SessionControlKey, connection: Any, messageId: String? = null): Boolean {
        val source = sources[key] ?: return false
        return source.connection === connection &&
            (messageId == null || source.completedMessageId == null || source.completedMessageId == messageId)
    }

    @Synchronized
    fun invalidateSession(key: SessionControlKey, connection: Any) {
        if (sources[key]?.connection === connection) sources.remove(key)
    }

    @Synchronized
    fun invalidateServer(serverId: String) {
        sources.keys.removeAll { it.serverId == serverId }
    }
}
