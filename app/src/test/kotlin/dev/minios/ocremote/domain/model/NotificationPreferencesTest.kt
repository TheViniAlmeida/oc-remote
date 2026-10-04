package dev.minios.ocremote.domain.model

import org.junit.Assert.*
import org.junit.Test

class NotificationPreferencesTest {
    @Test fun `mute and disabled category take priority over all timing modes`() {
        NotificationTiming.entries.forEach { timing ->
            assertFalse(shouldDeliverNotification(NotificationRule(timing = timing), true, false, false))
            assertFalse(shouldDeliverNotification(NotificationRule(enabled = false, timing = timing), false, false, false))
        }
    }

    @Test fun `timing separates active session and app foreground`() {
        assertTrue(shouldDeliverNotification(NotificationRule(timing = NotificationTiming.ALWAYS), false, true, true))
        assertFalse(shouldDeliverNotification(NotificationRule(), false, true, false))
        assertTrue(shouldDeliverNotification(NotificationRule(), false, false, true))
        assertFalse(shouldDeliverNotification(NotificationRule(timing = NotificationTiming.BACKGROUND_ONLY), false, false, true))
        assertTrue(shouldDeliverNotification(NotificationRule(timing = NotificationTiming.BACKGROUND_ONLY), false, false, false))
    }

    @Test fun `channel set is bounded and timing does not create new channels`() {
        val channels = mutableSetOf<String>()
        NotificationCategory.entries.forEach { category -> NotificationSound.entries.forEach { sound ->
            listOf(false, true).forEach { vibration -> NotificationTiming.entries.forEach { timing ->
                channels += notificationChannelId(category, NotificationRule(sound = sound, vibrate = vibration, timing = timing))
            } }
        } }
        assertEquals(16, channels.size)
    }

    @Test fun `legacy settings affect completion only and silent disables vibration`() {
        val settings = legacyNotificationPreferences(false, true)
        assertFalse(settings.agent.enabled)
        assertEquals(NotificationSound.SILENT, settings.agent.sound)
        assertFalse(settings.agent.vibrate)
        assertTrue(settings.permission.enabled)
        assertTrue(settings.question.enabled)
        assertTrue(settings.error.enabled)
    }

    @Test fun `quiet notifications preserve low interruption while requested vibration remains an alert`() {
        assertFalse(notificationRequiresAttention(legacyNotificationPreferences(true, true).agent))
        assertTrue(notificationRequiresAttention(NotificationRule(sound = NotificationSound.SILENT, vibrate = true)))
        assertTrue(notificationRequiresAttention(NotificationRule(sound = NotificationSound.DEFAULT, vibrate = false)))
    }
}
