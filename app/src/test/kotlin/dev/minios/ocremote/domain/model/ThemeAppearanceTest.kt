package dev.minios.ocremote.domain.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class ThemeAppearanceTest {
    @Test fun sessionOverridesOnlyItsFieldsAndInheritsProjectAndApp() {
        val app = ThemeAppearance("dark", "blue", dynamicColor = true, amoledDark = true)
        val project = AppearanceOverride(theme = "light", accent = "green")
        val session = AppearanceOverride(accent = "pink", dynamicColor = false)
        assertEquals(ThemeAppearance("light", "pink", false, true), resolveAppearance(app, project, session))
        assertEquals(ThemeAppearance("light", "green", true, true), resolveAppearance(app, project))
    }
    @Test fun invalidChoicesCannotOverrideValidParent() {
        val invalid = AppearanceOverride(theme = "unknown", accent = "invalid")
        assertTrue(invalid.normalized().isEmpty)
        assertEquals(ThemeAppearance("dark", "blue"), resolveAppearance(ThemeAppearance("dark", "blue"), invalid))
    }
    @Test fun explicitSystemAndDefaultAreDifferentFromInheritance() {
        assertEquals(ThemeAppearance(), resolveAppearance(ThemeAppearance("dark", "pink"), session = AppearanceOverride("system", "default")))
    }
    @Test fun nullAndFalseSurviveSerializationIndependently() {
        val override = AppearanceOverride(dynamicColor = false, amoledDark = null)
        assertEquals(override, Json.decodeFromString<AppearanceOverride>(Json.encodeToString(override)))
        assertFalse(override.isEmpty)
    }
}
