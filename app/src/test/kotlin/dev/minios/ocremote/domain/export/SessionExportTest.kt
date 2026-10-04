package dev.minios.ocremote.domain.export

import java.io.StringReader
import java.io.StringWriter
import org.junit.Assert.*
import org.junit.Test

class SessionExportTest {
    private fun render(data: String, format: SessionExportFormat) = StringWriter().also {
        renderSessionExport(StringReader(data), it, format, "A <title>")
    }.toString()
    @Test fun htmlEscapesContentAndNeverCreatesScriptOrAttachmentLinks() {
        val input = """{"info":{"title":"test"},"messages":[{"type":"user","text":"<script>alert('x')</script>&","files":[{"name":"test.png","data":"secret-binary","source":{"uri":"file:///tmp/test.png"}}]}]}"""
        val html = render(input, SessionExportFormat.HTML)
        assertFalse(html.contains("<script>"))
        assertTrue(html.contains("&lt;script&gt;"))
        assertTrue(html.contains("&amp;"))
        assertFalse(html.contains("secret-binary"))
        assertFalse(html.contains("href="))
        assertTrue(html.contains("test.png"))
    }
    @Test fun markdownPreservesV1UserAssistantToolContentAndArbitraryFences() {
        val input = """{"info":{"id":"s"},"messages":[{"info":{"role":"assistant"},"parts":[{"type":"text","text":"```\n<script>\n````"},{"type":"tool","state":{"output":"tool result"}}]}]}"""
        val md = render(input, SessionExportFormat.MARKDOWN)
        assertTrue(md.contains("    ```\n    <script>\n    ````"))
        assertTrue(md.contains("    tool result"))
        assertTrue(md.contains("## Message 1"))
    }
    @Test fun fullNativeSessionBeyondDefaultHistoryIsConverted() {
        val data = """{"info":{},"messages":[""" + (1..125).joinToString(",") { """{"text":"message-$it"}""" } + "]}"
        assertTrue(render(data, SessionExportFormat.HTML).contains("message-125"))
        assertTrue(render(data, SessionExportFormat.MARKDOWN).contains("## Message 125"))
        validateNativeSessionExport(StringReader(data))
    }
    @Test fun clipboardLimitFailsExplicitlyWithoutTruncation() {
        val writer = ClipboardExportWriter(10)
        writer.write("1234567890")
        assertThrows(SessionExportTooLargeException::class.java) { writer.write("x") }
        assertEquals("1234567890", writer.toString())
    }
    @Test fun serverErrorAndTruncatedDocumentsCannotReportSuccessfulExport() {
        assertThrows(IllegalArgumentException::class.java) { validateNativeSessionExport(StringReader("""{"error":"Denied"}""")) }
        assertThrows(Exception::class.java) { validateNativeSessionExport(StringReader("""{"info":{},"messages": [""")) }
    }

    private class GeneratedLiteralReader(private val literalLength: Int, private val binary: Boolean = false) : java.io.Reader() {
        private val prefix = if (binary) "{\"info\":{},\"messages\":[{\"files\":[{\"data\":\"" else "{\"info\":{},\"messages\":[{\"text\":\""
        private val suffix = if (binary) "\"}]}]}" else "\"}]}"
        var consumed = 0
            private set
        override fun read(buffer: CharArray, offset: Int, length: Int): Int {
            val total = prefix.length + literalLength + suffix.length
            if (consumed == total) return -1
            val count = minOf(length, total - consumed)
            repeat(count) { index ->
                val position = consumed + index
                buffer[offset + index] = when {
                    position < prefix.length -> prefix[position]
                    position < prefix.length + literalLength -> 'x'
                    else -> suffix[position - prefix.length - literalLength]
                }
            }
            consumed += count
            return count
        }
        override fun close() = Unit
    }

    @Test fun megabyteScalarIsRejectedDuringReadingBeforeGsonMaterializesIt() {
        for (format in listOf(SessionExportFormat.MARKDOWN, SessionExportFormat.HTML)) {
            val input = GeneratedLiteralReader(1024 * 1024)
            assertThrows(SessionExportConversionLimitException::class.java) {
                renderSessionExport(input, StringWriter(), format, "test")
            }
            assertTrue(input.consumed in (64 * 1024)..(68 * 1024))
        }
    }

    @Test fun skippedBinaryPayloadAlsoObeysLexicalLimits() {
        val input = GeneratedLiteralReader(1024 * 1024, binary = true)
        assertThrows(SessionExportConversionLimitException::class.java) {
            renderSessionExport(input, StringWriter(), SessionExportFormat.HTML, "test")
        }
        assertTrue(input.consumed < 68 * 1024)
    }

    @Test fun jsonValidationDoesNotApplyReadableScalarLimitOrTruncateNativeDocument() {
        val input = GeneratedLiteralReader(1024 * 1024)
        validateNativeSessionExport(input)
        assertTrue(input.consumed > 1024 * 1024)
    }

    @Test fun escapedQuotesAndBracketsInsideStringDoNotAffectLexicalDepth() {
        val data = """{"info":{},"messages":[{"text":"quoted \" braces [ { } ] slash \\"}]}"""
        val html = render(data, SessionExportFormat.HTML)
        assertTrue(html.contains("quoted &quot; braces [ { } ] slash"))
    }

    @Test fun pathologicalNestingAndFieldPathsFailExplicitly() {
        val nested = "{\"info\":{},\"messages\":[" + "[".repeat(70) + "0" + "]".repeat(70) + "]}"
        assertThrows(SessionExportConversionLimitException::class.java) { render(nested, SessionExportFormat.HTML) }
        val hugeKey = "{\"info\":{},\"messages\":[{\"" + "x".repeat(9 * 1024) + "\":0}]}"
        assertThrows(SessionExportConversionLimitException::class.java) { render(hugeKey, SessionExportFormat.MARKDOWN) }
    }

    @Test fun toolArgumentsNamedUrlDataAndFilesArePreservedWhileFileAttachmentsAreOmitted() {
        val data = """{"info":{},"messages":[{"parts":[{"type":"tool","state":{"input":{"url":"https://example.invalid/argument","data":"tool argument","files":[{"data":"another argument"}]} }},{"type":"file","url":"data:image/png;base64,ATTACHMENT"}]}]}"""
        for (format in listOf(SessionExportFormat.MARKDOWN, SessionExportFormat.HTML)) {
            val result = render(data, format)
            assertTrue(result.contains("https://example.invalid/argument"))
            assertTrue(result.contains("tool argument"))
            assertTrue(result.contains("another argument"))
            assertFalse(result.contains("data:image/png;base64,ATTACHMENT"))
        }
    }
}
