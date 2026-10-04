package dev.minios.ocremote.domain.export

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.Reader
import java.io.Writer

enum class SessionExportFormat(val extension: String, val mimeType: String) {
    MARKDOWN("md", "text/markdown"), JSON("json", "application/json"), HTML("html", "text/html")
}

class SessionExportTooLargeException : java.io.IOException("Session is too large for the clipboard; download it instead")

class SessionExportConversionLimitException : java.io.IOException("Readable export exceeds safe conversion limits; download native JSON instead")

private const val MAX_EXPORT_LITERAL_CHARS = 64 * 1024
private const val MAX_EXPORT_DEPTH = 64
private const val MAX_EXPORT_PATH_CHARS = 8 * 1024
private val ATTACHMENT_OBJECT_PATH = Regex("messages\\[\\d+](?:\\.files|\\.attachments)\\[\\d+]")
private val MESSAGE_PART_OBJECT_PATH = Regex("messages\\[\\d+]\\.parts\\[\\d+]")

/** Enforces lexical limits before Gson can allocate an arbitrarily large scalar string. */
internal class SessionExportReaderGuard(private val input: Reader) : Reader() {
    private var depth = 0
    private var inString = false
    private var escaped = false
    private var literalChars = 0

    override fun read(buffer: CharArray, offset: Int, length: Int): Int {
        val count = input.read(buffer, offset, length.coerceAtMost(1024))
        if (count <= 0) return count
        for (index in offset until offset + count) {
            val char = buffer[index]
            if (inString) {
                if (!escaped && char == '"') {
                    inString = false
                    literalChars = 0
                    continue
                }
                if (++literalChars > MAX_EXPORT_LITERAL_CHARS) throw SessionExportConversionLimitException()
                escaped = if (escaped) false else char == '\\'
            } else {
                when (char) {
                    '"' -> { inString = true; escaped = false; literalChars = 0 }
                    '{', '[' -> {
                        literalChars = 0
                        if (++depth > MAX_EXPORT_DEPTH) throw SessionExportConversionLimitException()
                    }
                    '}', ']' -> { depth--; literalChars = 0 }
                    ',', ':', ' ', '\n', '\r', '\t' -> literalChars = 0
                    else -> if (++literalChars > MAX_EXPORT_LITERAL_CHARS) throw SessionExportConversionLimitException()
                }
            }
        }
        return count
    }

    override fun close() = input.close()
}

/** Limits UTF-16 characters and UTF-8 bytes conservatively below Android's Binder transaction limit. */
internal class ClipboardExportWriter(private val limit: Int = 64 * 1024) : Writer() {
    private val content = StringBuilder()
    override fun write(buffer: CharArray, offset: Int, length: Int) {
        if (content.length.toLong() + length > limit) throw SessionExportTooLargeException()
        content.append(buffer, offset, length)
    }
    override fun flush() = Unit
    override fun close() = Unit
    override fun toString() = content.toString()
}

internal fun htmlEscape(value: String): String = buildString(value.length) {
    value.forEach { append(when (it) { '&' -> "&amp;"; '<' -> "&lt;"; '>' -> "&gt;"; '"' -> "&quot;"; '\'' -> "&#39;"; else -> it.toString() }) }
}

private fun markdownLabel(value: String): String = value.replace("\\", "\\\\")
    .replace("*", "\\*").replace("_", "\\_").replace("[", "\\[").replace("]", "\\]")
    .replace("<", "&lt;").replace(">", "&gt;").replace("\n", " ").replace("\r", " ")

/**
 * Converts the server's complete, native V1/V2 export without collecting the session in memory.
 * Every scalar activity field is retained as labelled text; JSON retains the native document verbatim.
 * Inline binary attachment data and URLs are omitted from readable formats, never embedded as HTML.
 * Readable conversion fails above 64 KiB per literal, depth 64 or an 8 KiB field path; JSON is unaffected.
 */
internal fun renderSessionExport(input: Reader, output: Writer, format: SessionExportFormat, title: String) {
    require(format != SessionExportFormat.JSON) { "Native JSON must be copied without conversion" }
    if (title.length > MAX_EXPORT_LITERAL_CHARS) throw SessionExportConversionLimitException()
    val html = format == SessionExportFormat.HTML
    if (html) {
        output.write("<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width\"><title>${htmlEscape(title)}</title><style>body{font:16px system-ui;max-width:960px;margin:2em auto;padding:0 1em}pre{white-space:pre-wrap;overflow-wrap:anywhere;background:#f2f2f2;padding:1em}h3{overflow-wrap:anywhere}</style></head><body><h1>${htmlEscape(title)}</h1>")
    } else output.write("# ${markdownLabel(title)}\n\n")
    val note = "Inline attachment data and attachment URLs are included in the JSON export."
    if (html) output.write("<p>$note</p>") else output.write("$note\n\n")
    fun scalar(path: String, value: String) {
        if (html) output.write("<h3>${htmlEscape(path)}</h3><pre>${htmlEscape(value)}</pre>\n")
        else {
            output.write("**${markdownLabel(path)}**\n\n")
            // Indented blocks preserve arbitrary backticks, HTML and markdown without executing them.
            value.splitToSequence('\n').forEach { output.write("    $it\n") }
            output.write("\n")
        }
    }
    val reader = JsonReader(SessionExportReaderGuard(input)).apply { isLenient = false }
    fun value(path: String, depth: Int) {
        if (depth > MAX_EXPORT_DEPTH || path.length > MAX_EXPORT_PATH_CHARS) throw SessionExportConversionLimitException()
        when (reader.peek()) {
            JsonToken.BEGIN_OBJECT -> {
                reader.beginObject()
                var attachmentObject = ATTACHMENT_OBJECT_PATH.matches(path)
                val messagePartObject = MESSAGE_PART_OBJECT_PATH.matches(path)
                while (reader.hasNext()) {
                    val name = reader.nextName()
                    if (path.length.toLong() + name.length + 1 > MAX_EXPORT_PATH_CHARS) throw SessionExportConversionLimitException()
                    val child = if (path.isEmpty()) name else "$path.$name"
                    if (messagePartObject && name == "type" && reader.peek() == JsonToken.STRING) {
                        val type = reader.nextString()
                        attachmentObject = type == "file"
                        scalar(child, type)
                    } else if (attachmentObject && name in setOf("url", "data", "base64")) {
                        reader.skipValue()
                        scalar(child, "[attachment payload omitted; available in JSON]")
                    } else value(child, depth + 1)
                }
                reader.endObject()
            }
            JsonToken.BEGIN_ARRAY -> {
                reader.beginArray()
                var index = 0
                while (reader.hasNext()) {
                    if (path == "messages") {
                        if (html) output.write("<section><h2>Message ${index + 1}</h2>\n")
                        else output.write("## Message ${index + 1}\n\n")
                    }
                    value("$path[$index]", depth + 1)
                    if (path == "messages" && html) output.write("</section>\n")
                    index++
                }
                reader.endArray()
            }
            JsonToken.NULL -> { reader.nextNull(); scalar(path, "null") }
            JsonToken.BOOLEAN -> scalar(path, reader.nextBoolean().toString())
            JsonToken.STRING, JsonToken.NUMBER -> scalar(path, reader.nextString())
            else -> throw java.io.IOException("Invalid native session export")
        }
    }
    value("", 0)
    require(reader.peek() == JsonToken.END_DOCUMENT) { "Trailing export content" }
    if (html) output.write("</body></html>\n")
    output.flush()
}

/** Fail before returning success when an endpoint returned an error document instead of a session. */
internal fun validateNativeSessionExport(input: Reader) {
    val reader = JsonReader(input).apply { isLenient = false }
    require(reader.peek() == JsonToken.BEGIN_OBJECT) { "Invalid session export" }
    var info = false
    var messages = false
    reader.beginObject()
    while (reader.hasNext()) {
        when (reader.nextName()) {
            "info" -> { info = reader.peek() == JsonToken.BEGIN_OBJECT; reader.skipValue() }
            "messages" -> { messages = reader.peek() == JsonToken.BEGIN_ARRAY; reader.skipValue() }
            else -> reader.skipValue()
        }
    }
    reader.endObject()
    require(info && messages && reader.peek() == JsonToken.END_DOCUMENT) { "Incomplete native session export" }
}
