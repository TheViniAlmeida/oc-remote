package dev.minios.ocremote.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import dev.minios.ocremote.R
import dev.minios.ocremote.domain.model.NotificationCategory
import dev.minios.ocremote.domain.model.NotificationRule
import dev.minios.ocremote.domain.model.NotificationSound
import dev.minios.ocremote.domain.model.notificationChannelId
import dev.minios.ocremote.domain.model.notificationRequiresAttention

internal fun notificationCategoryLabel(category: NotificationCategory): Int = when (category) {
    NotificationCategory.AGENT -> R.string.notification_preferences_agent
    NotificationCategory.PERMISSION -> R.string.notification_preferences_permissions
    NotificationCategory.QUESTION -> R.string.notification_preferences_questions
    NotificationCategory.ERROR -> R.string.notification_preferences_errors
}

internal fun ensurePreferenceNotificationChannel(context: Context, category: NotificationCategory, rule: NotificationRule): String {
    val id = notificationChannelId(category, rule)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val soundLabel = context.getString(if (rule.sound == NotificationSound.SILENT) R.string.notification_preferences_silent else R.string.notification_preferences_default_sound)
        val vibrationLabel = context.getString(if (rule.vibrate) R.string.notification_preferences_vibrate else R.string.notification_preferences_no_vibration)
        val channel = NotificationChannel(
            id,
            "${context.getString(notificationCategoryLabel(category))} · $soundLabel · $vibrationLabel",
            if (notificationRequiresAttention(rule)) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_LOW,
        ).apply {
            setSound(
                if (rule.sound == NotificationSound.DEFAULT) RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) else null,
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build(),
            )
            enableVibration(rule.vibrate)
            if (rule.vibrate) vibrationPattern = longArrayOf(0, 300, 100, 300)
        }
        // Existing channels retain system-selected ringtone, importance, and vibration settings.
        manager.createNotificationChannel(channel)
    }
    return id
}
