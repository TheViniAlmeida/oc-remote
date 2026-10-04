package dev.minios.ocremote.data.repository

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import org.junit.Assert.*
import org.junit.Test

class AutomaticScrollingPreferenceTest {
    @Test fun missingSettingPreservesExistingAutomaticScrolling() {
        assertTrue(SettingsRepository.automaticScrollingEnabled(emptyPreferences()))
    }
    @Test fun explicitDisabledAndEnabledAreHonored() {
        val key = booleanPreferencesKey("auto_scroll_enabled")
        assertFalse(SettingsRepository.automaticScrollingEnabled(mutablePreferencesOf(key to false)))
        assertTrue(SettingsRepository.automaticScrollingEnabled(mutablePreferencesOf(key to true)))
    }
}
