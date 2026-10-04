package dev.minios.ocremote.domain.model

import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
enum class TimelineCategory { SHELL, EDITS, REASONING, SUBAGENTS, NOTICES, OTHER_TOOLS }

@Serializable
enum class TimelinePreset { MINIMAL, COMPACT, BALANCED, DETAILED, CUSTOM }

@Serializable
data class TimelineActivity(
    val visible: Boolean = true,
    val grouped: Boolean = false,
    val collapsed: Boolean = true,
)

@Serializable
data class TimelinePreferences(
    val preset: TimelinePreset = TimelinePreset.BALANCED,
    val shell: TimelineActivity = TimelineActivity(),
    val edits: TimelineActivity = TimelineActivity(),
    val reasoning: TimelineActivity = TimelineActivity(),
    val subagents: TimelineActivity = TimelineActivity(),
    val notices: TimelineActivity = TimelineActivity(collapsed = false),
    val otherTools: TimelineActivity = TimelineActivity(),
) {
    fun activity(category: TimelineCategory): TimelineActivity = when (category) {
        TimelineCategory.SHELL -> shell
        TimelineCategory.EDITS -> edits
        TimelineCategory.REASONING -> reasoning
        TimelineCategory.SUBAGENTS -> subagents
        TimelineCategory.NOTICES -> notices
        TimelineCategory.OTHER_TOOLS -> otherTools
    }

    fun withActivity(category: TimelineCategory, activity: TimelineActivity): TimelinePreferences = when (category) {
        TimelineCategory.SHELL -> copy(preset = TimelinePreset.CUSTOM, shell = activity)
        TimelineCategory.EDITS -> copy(preset = TimelinePreset.CUSTOM, edits = activity)
        TimelineCategory.REASONING -> copy(preset = TimelinePreset.CUSTOM, reasoning = activity)
        TimelineCategory.SUBAGENTS -> copy(preset = TimelinePreset.CUSTOM, subagents = activity)
        TimelineCategory.NOTICES -> copy(preset = TimelinePreset.CUSTOM, notices = activity)
        TimelineCategory.OTHER_TOOLS -> copy(preset = TimelinePreset.CUSTOM, otherTools = activity)
    }

    /** Fixed presets cannot be mislabeled by stale serialized per-category overrides. */
    fun normalized(): TimelinePreferences = if (preset == TimelinePreset.CUSTOM) this else forPreset(preset)

    companion object {
        fun forPreset(preset: TimelinePreset): TimelinePreferences {
            val balanced = TimelinePreferences()
            return when (preset) {
                TimelinePreset.MINIMAL -> balanced.copy(
                    preset = preset, shell = TimelineActivity(visible = false),
                    edits = TimelineActivity(visible = false), reasoning = TimelineActivity(visible = false),
                    subagents = TimelineActivity(visible = false), otherTools = TimelineActivity(visible = false),
                )
                TimelinePreset.COMPACT -> balanced.copy(
                    preset = preset, shell = TimelineActivity(grouped = true), edits = TimelineActivity(grouped = true),
                    reasoning = TimelineActivity(visible = false), subagents = TimelineActivity(grouped = true),
                    notices = TimelineActivity(grouped = true, collapsed = false), otherTools = TimelineActivity(grouped = true),
                )
                TimelinePreset.BALANCED -> balanced
                TimelinePreset.DETAILED -> balanced.copy(
                    preset = preset, shell = TimelineActivity(collapsed = false), edits = TimelineActivity(collapsed = false),
                    reasoning = TimelineActivity(collapsed = false), subagents = TimelineActivity(collapsed = false),
                    otherTools = TimelineActivity(collapsed = false),
                )
                TimelinePreset.CUSTOM -> balanced.copy(preset = preset)
            }
        }
    }
}

fun resolveTimeline(server: TimelinePreferences, session: TimelinePreferences?): TimelinePreferences =
    (session ?: server).normalized()

/** Text and attachments have no activity category and always retain their position. */
fun timelineCategory(part: Part): TimelineCategory? = when (part) {
    is Part.Reasoning -> TimelineCategory.REASONING
    is Part.Patch -> TimelineCategory.EDITS
    is Part.Subtask, is Part.Agent -> TimelineCategory.SUBAGENTS
    is Part.Permission, is Part.Question, is Part.Abort, is Part.Retry, is Part.Compaction -> TimelineCategory.NOTICES
    is Part.Tool -> {
        val name = part.tool.lowercase(Locale.ROOT).substringAfterLast('.')
        when {
            part.state is ToolState.Error -> TimelineCategory.NOTICES
            name in setOf("bash", "shell", "sh", "exec", "terminal", "execute", "exec_command") -> TimelineCategory.SHELL
            name in setOf("edit", "write", "apply_patch", "multiedit", "patch", "replace", "create_file") -> TimelineCategory.EDITS
            name in setOf("task", "agent", "subagent", "delegate", "spawn_agent", "parallel") -> TimelineCategory.SUBAGENTS
            name in setOf("warning", "warn", "error", "notice", "notification") -> TimelineCategory.NOTICES
            else -> TimelineCategory.OTHER_TOOLS
        }
    }
    else -> null
}

fun isTimelinePartVisible(part: Part, preferences: TimelinePreferences, isUser: Boolean = false): Boolean {
    // Required interactions and user content must never disappear because of activity preferences.
    if (isUser || part is Part.Permission || part is Part.Question) return true
    return timelineCategory(part)?.let { preferences.activity(it).visible } ?: true
}

data class TimelinePartGroup(val category: TimelineCategory?, val parts: List<Part>, val grouped: Boolean)

/** Only neighboring activities of one category merge. Messages/attachments break groups. */
fun groupTimelineParts(parts: List<Part>, preferences: TimelinePreferences, isUser: Boolean = false): List<TimelinePartGroup> {
    val result = mutableListOf<TimelinePartGroup>()
    parts.filter { isTimelinePartVisible(it, preferences, isUser) }.forEach { part ->
        val category = timelineCategory(part)
        val grouped = !isUser && category != null && preferences.activity(category).grouped &&
            part !is Part.Permission && part !is Part.Question
        val previous = result.lastOrNull()
        if (grouped && previous?.grouped == true && previous.category == category) {
            result[result.lastIndex] = previous.copy(parts = previous.parts + part)
        } else result += TimelinePartGroup(category, listOf(part), grouped)
    }
    return result
}
