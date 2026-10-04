package dev.minios.ocremote.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class NotificationCategory { AGENT, PERMISSION, QUESTION, ERROR }

@Serializable
enum class NotificationSound { DEFAULT, SILENT }

@Serializable
enum class NotificationTiming { ALWAYS, OUTSIDE_ACTIVE_SESSION, BACKGROUND_ONLY }

@Serializable
data class NotificationRule(
    val enabled: Boolean = true,
    val sound: NotificationSound = NotificationSound.DEFAULT,
    val vibrate: Boolean = true,
    val timing: NotificationTiming = NotificationTiming.OUTSIDE_ACTIVE_SESSION,
)

@Serializable
data class NotificationPreferences(
    val agent: NotificationRule = NotificationRule(),
    val permission: NotificationRule = NotificationRule(),
    val question: NotificationRule = NotificationRule(),
    val error: NotificationRule = NotificationRule(),
) {
    fun rule(category: NotificationCategory): NotificationRule = when (category) {
        NotificationCategory.AGENT -> agent
        NotificationCategory.PERMISSION -> permission
        NotificationCategory.QUESTION -> question
        NotificationCategory.ERROR -> error
    }

    fun withRule(category: NotificationCategory, rule: NotificationRule): NotificationPreferences = when (category) {
        NotificationCategory.AGENT -> copy(agent = rule)
        NotificationCategory.PERMISSION -> copy(permission = rule)
        NotificationCategory.QUESTION -> copy(question = rule)
        NotificationCategory.ERROR -> copy(error = rule)
    }
}

/** Empty identity is app scope; a session is always scoped to its exact server. */
@Serializable
data class NotificationPreferenceScope(val serverId: String? = null, val sessionId: String? = null) {
    init {
        require(serverId == null || serverId.isNotBlank())
        require(sessionId == null || sessionId.isNotBlank())
        require(sessionId == null || serverId != null)
    }
}

fun legacyNotificationPreferences(taskEnabled: Boolean, taskSilent: Boolean): NotificationPreferences =
    NotificationPreferences(agent = NotificationRule(
        enabled = taskEnabled,
        sound = if (taskSilent) NotificationSound.SILENT else NotificationSound.DEFAULT,
        vibrate = !taskSilent,
    ))

fun shouldDeliverNotification(rule: NotificationRule, muted: Boolean, activeSession: Boolean, appForeground: Boolean): Boolean {
    if (muted || !rule.enabled) return false
    return when (rule.timing) {
        NotificationTiming.ALWAYS -> true
        NotificationTiming.OUTSIDE_ACTIVE_SESSION -> !activeSession
        NotificationTiming.BACKGROUND_ONLY -> !appForeground
    }
}

/** A fixed set of 16 channels avoids accumulating channels on every settings edit. */
fun notificationChannelId(category: NotificationCategory, rule: NotificationRule): String =
    "oc_event_v1_${category.name.lowercase()}_${rule.sound.name.lowercase()}_${if (rule.vibrate) "vibrate" else "still"}"

/** A fully quiet notification should also retain the legacy low visual interruption level. */
fun notificationRequiresAttention(rule: NotificationRule): Boolean = rule.sound != NotificationSound.SILENT || rule.vibrate
