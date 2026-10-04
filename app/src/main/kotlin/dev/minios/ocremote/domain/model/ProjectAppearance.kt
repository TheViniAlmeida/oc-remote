package dev.minios.ocremote.domain.model

import kotlinx.serialization.Serializable
import java.util.Locale

const val MAX_PROJECT_TAGS = 12
const val MAX_PROJECT_TAG_LENGTH = 32

@Serializable
data class ProjectAppearance(
    val color: String? = null,
    val tags: List<String> = emptyList(),
) {
    fun normalized(): ProjectAppearance = copy(
        color = color?.takeIf { it in projectColorKeys },
        tags = tags.map { it.trim().take(MAX_PROJECT_TAG_LENGTH).trim() }
            .filter(String::isNotBlank)
            .distinctBy { it.lowercase(Locale.ROOT) }
            .take(MAX_PROJECT_TAGS),
    )

    val isEmpty: Boolean get() = color == null && tags.isEmpty()
}

private val projectColorKeys = setOf("red", "orange", "amber", "green", "teal", "blue", "violet", "pink")

/** Lexical normalization only: remote paths must never resolve against the phone filesystem. */
fun normalizeProjectDirectory(path: String): String {
    val segments = mutableListOf<String>()
    path.split('/').forEach { segment ->
        when (segment) {
            "", "." -> Unit
            ".." -> if (segments.isNotEmpty()) segments.removeAt(segments.lastIndex)
            else -> segments.add(segment)
        }
    }
    return "/" + segments.joinToString("/")
}

fun projectDirectoryName(path: String): String = normalizeProjectDirectory(path)
    .substringAfterLast('/').ifEmpty { "/" }

fun parentProjectDirectory(path: String): String? = normalizeProjectDirectory(path)
    .takeUnless { it == "/" }?.substringBeforeLast('/')?.ifEmpty { "/" }

fun projectAppearanceId(projectId: String, directory: String): String =
    projectId.takeIf { it.isNotBlank() && !it.startsWith("directory:") }
        ?: "directory:${normalizeProjectDirectory(directory)}"
