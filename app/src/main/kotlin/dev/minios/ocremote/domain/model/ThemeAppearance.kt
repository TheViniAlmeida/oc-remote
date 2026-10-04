package dev.minios.ocremote.domain.model

import kotlinx.serialization.Serializable

val appAccentKeys = listOf("default", "red", "orange", "amber", "green", "teal", "blue", "violet", "pink")
val appThemeKeys = listOf("system", "light", "dark")

@Serializable
data class ThemeAppearance(
    val theme: String = "system",
    val accent: String = "default",
    val dynamicColor: Boolean = false,
    val amoledDark: Boolean = false,
) {
    fun normalized() = copy(
        theme = theme.takeIf { it in appThemeKeys } ?: "system",
        accent = accent.takeIf { it in appAccentKeys } ?: "default",
    )
}

/** Null fields inherit the next broader scope. A null object removes the scope entirely. */
@Serializable
data class AppearanceOverride(
    val theme: String? = null,
    val accent: String? = null,
    val dynamicColor: Boolean? = null,
    val amoledDark: Boolean? = null,
) {
    fun normalized() = copy(
        theme = theme?.takeIf { it in appThemeKeys },
        accent = accent?.takeIf { it in appAccentKeys },
    )
    val isEmpty: Boolean get() = theme == null && accent == null && dynamicColor == null && amoledDark == null
}

fun resolveAppearance(
    app: ThemeAppearance,
    project: AppearanceOverride? = null,
    session: AppearanceOverride? = null,
): ThemeAppearance {
    fun ThemeAppearance.apply(value: AppearanceOverride?): ThemeAppearance {
        val override = value?.normalized() ?: return this
        return copy(
            theme = override.theme ?: theme,
            accent = override.accent ?: accent,
            dynamicColor = override.dynamicColor ?: dynamicColor,
            amoledDark = override.amoledDark ?: amoledDark,
        )
    }
    return app.normalized().apply(project).apply(session)
}
