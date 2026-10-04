package dev.minios.ocremote.service

import android.app.Notification
import android.app.NotificationManager
import android.content.Context

private const val TASK_COMPLETE_OFFSET = 0
private const val PERMISSION_OFFSET = 1000
private const val QUESTION_OFFSET = 2000
private const val ERROR_OFFSET = 3000

internal fun eventNotificationId(serverId: String, sessionId: String, typeOffset: Int): Int =
    (serverId + sessionId).hashCode() + typeOffset

internal fun sessionEventNotificationIds(serverId: String, sessionId: String): List<Int> = listOf(
    eventNotificationId(serverId, sessionId, TASK_COMPLETE_OFFSET),
    eventNotificationId(serverId, sessionId, PERMISSION_OFFSET),
    eventNotificationId(serverId, sessionId, QUESTION_OFFSET),
    eventNotificationId(serverId, sessionId, ERROR_OFFSET),
)

internal fun shouldPostSessionNotification(
    activeSession: Pair<String, String>?,
    serverId: String,
    sessionId: String,
    mutedSessions: Set<Pair<String, String>> = emptySet(),
): Boolean = activeSession != (serverId to sessionId) && (serverId to sessionId) !in mutedSessions

internal object SessionNotificationCoordinator {
    private var activeSession: Pair<String, String>? = null
    private var mutedSessions: Set<Pair<String, String>> = emptySet()

    @Synchronized
    fun updateMutedSessions(context: Context, sessions: Set<Pair<String, String>>) {
        updateMutedSessions(sessions) { serverId, sessionId -> clearSession(context, serverId, sessionId) }
    }

    @Synchronized
    internal fun updateMutedSessions(sessions: Set<Pair<String, String>>, clear: (String, String) -> Unit) {
        val newlyMuted = sessions - mutedSessions
        mutedSessions = sessions
        newlyMuted.forEach { (serverId, sessionId) -> clear(serverId, sessionId) }
    }

    @Synchronized
    fun activate(context: Context, serverId: String, sessionId: String) {
        activeSession = serverId to sessionId
        clearSession(context, serverId, sessionId)
    }

    @Synchronized
    fun clearSession(context: Context, serverId: String, sessionId: String) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        sessionEventNotificationIds(serverId, sessionId).forEach(notificationManager::cancel)

        val groupKey = "server_$serverId"
        val hasRemainingChildren = notificationManager.activeNotifications.any {
            it.notification.group == groupKey &&
                it.notification.flags and Notification.FLAG_GROUP_SUMMARY == 0
        }
        if (!hasRemainingChildren) {
            notificationManager.cancel(serverGroupSummaryNotificationId(serverId))
        }
    }

    @Synchronized
    fun deactivate(serverId: String, sessionId: String) {
        if (activeSession == serverId to sessionId) {
            activeSession = null
        }
    }

    @Synchronized
    fun postUnlessActive(serverId: String, sessionId: String, post: () -> Unit): Boolean {
        if (!shouldPostSessionNotification(activeSession, serverId, sessionId, mutedSessions)) return false
        post()
        return true
    }
}

internal fun serverGroupSummaryNotificationId(serverId: String): Int =
    "server_summary_$serverId".hashCode()
