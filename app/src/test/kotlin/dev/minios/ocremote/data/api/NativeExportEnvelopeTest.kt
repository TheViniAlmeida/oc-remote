package dev.minios.ocremote.data.api

import org.junit.Assert.*
import org.junit.Test
import java.io.Reader
import java.io.StringReader
import java.io.StringWriter
import java.io.Writer

class NativeExportEnvelopeTest {
    @Test fun preservesNativePayloadEscapesNumbersWhitespaceAndNullsVerbatim() {
        val payload = """{ "info":{"id":"s","nullable":null}, "messages":[{"text":"quotes: \"}\\ unicode: \u00e7","value":-1.5e+23,"done":true}]}"""
        val output = StringWriter()
        unwrapV2DataJson(StringReader("{\"ignored\":[1,{\"x\":\"skip\"}],\"data\":$payload,\"other\":false}"), output)
        assertEquals(payload, output.toString())
    }

    private class GeneratedExportReader(private val size: Int) : Reader() {
        private val prefix = "{\"data\":{\"info\":{},\"messages\":[{\"text\":\""
        private val suffix = "\"}]},\"ignored\":null}"
        var consumed = 0
            private set
        var largestRequestedRead = 0
            private set
        override fun read(buffer: CharArray, offset: Int, length: Int): Int {
            largestRequestedRead = maxOf(largestRequestedRead, length)
            val total = prefix.length + size + suffix.length
            if (consumed == total) return -1
            val count = minOf(length, total - consumed)
            repeat(count) { index ->
                val position = consumed + index
                buffer[offset + index] = when {
                    position < prefix.length -> prefix[position]
                    position < prefix.length + size -> 'x'
                    else -> suffix[position - prefix.length - size]
                }
            }
            consumed += count
            return count
        }
        override fun close() = Unit
    }
    private class CountingWriter : Writer() {
        var chars = 0
        var payloadChars = 0
        var largestWrite = 0
        override fun write(buffer: CharArray, offset: Int, length: Int) {
            chars += length
            largestWrite = maxOf(largestWrite, length)
            for (index in offset until offset + length) if (buffer[index] == 'x') payloadChars++
        }
        override fun flush() = Unit
        override fun close() = Unit
    }

    @Test fun megabyteScalarIsCopiedWithBoundedBuffersAndNoScalarString() {
        val input = GeneratedExportReader(1024 * 1024)
        val output = CountingWriter()
        unwrapV2DataJson(input, output)
        assertEquals(1024 * 1024 + 1, output.payloadChars) // One x also occurs in the field name text.
        assertEquals(1024 * 1024 + "{\"info\":{},\"messages\":[{\"text\":\"\"}]}".length, output.chars)
        assertTrue(input.largestRequestedRead <= 8192)
        assertTrue(output.largestWrite <= 8192)
        assertTrue(input.consumed > 1024 * 1024)
    }

    @Test fun malformedDuplicateMissingAndTrailingEnvelopesFailExplicitly() {
        val invalid = listOf(
            """{"data":{},"data":{}}""", """{"other":{}}""", """{"data":{}} trailing""",
            """{"data":{"x":"\uZZZZ"}}""", """{"data":{"x":01}}""", """{"data":[1,]}""",
            """{"data":{"x":"unterminated}}""",
        )
        invalid.forEach { data ->
            assertThrows(Exception::class.java) { unwrapV2DataJson(StringReader(data), StringWriter()) }
        }
    }

    @Test fun envelopeKeysAndNestedDepthHaveFixedLimits() {
        val key = "{\"" + "a".repeat(1025) + "\":0,\"data\":{}}"
        assertThrows(Exception::class.java) { unwrapV2DataJson(StringReader(key), StringWriter()) }
        val deep = "{\"data\":" + "[".repeat(140) + "0" + "]".repeat(140) + "}"
        assertThrows(Exception::class.java) { unwrapV2DataJson(StringReader(deep), StringWriter()) }
    }
}
