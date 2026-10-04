package dev.minios.ocremote.data.api

import java.io.BufferedReader
import java.io.Reader
import java.io.Writer

/** Streaming JSON envelope parser; payload strings are copied as raw characters, never allocated. */
internal class NativeExportEnvelope(input: Reader, output: Writer) {
    private val input = if (input is BufferedReader) input else input.buffered(8192)
    private val output = BufferedExportOutput(output)
    private var lookahead = -2

    private fun peek(): Int {
        if (lookahead == -2) lookahead = input.read()
        return lookahead
    }
    private fun take(): Int = peek().also { lookahead = -2 }
    private fun expect(char: Char, copy: Boolean = false) {
        require(take() == char.code) { "Invalid OpenCode export JSON" }
        if (copy) output.write(char)
    }
    private fun whitespace(copy: Boolean = false) {
        while (peek() in listOf(' '.code, '\t'.code, '\r'.code, '\n'.code)) {
            val char = take().toChar()
            if (copy) output.write(char)
        }
    }

    fun unwrap() {
        whitespace()
        expect('{')
        whitespace()
        var found = false
        if (peek() != '}'.code) {
            while (true) {
                val name = StringBuilder()
                string(copy = false, key = true, decoded = name)
                whitespace()
                expect(':')
                whitespace()
                val payload = name.toString() == "data"
                require(!payload || !found) { "Duplicate OpenCode export payload" }
                value(payload, depth = 0)
                if (payload) found = true
                whitespace()
                if (peek() != ','.code) break
                take()
                whitespace()
            }
        }
        expect('}')
        whitespace()
        require(found) { "Missing OpenCode export payload" }
        require(peek() == -1) { "Trailing OpenCode export content" }
        output.flush()
    }

    private fun string(copy: Boolean, key: Boolean = false, decoded: StringBuilder? = null) {
        expect('"', copy)
        var keyChars = 0
        fun keyCharacter(char: Char) {
            if (key && ++keyChars > 1024) throw java.io.IOException("OpenCode export field name exceeds safe limits")
            decoded?.append(char)
        }
        while (true) {
            val code = take()
            require(code >= 0x20) { "Invalid OpenCode export string" }
            val char = code.toChar()
            if (copy) output.write(char)
            when (char) {
                '"' -> return
                '\\' -> {
                    val escapeCode = take()
                    require(escapeCode != -1) { "Unterminated OpenCode export string" }
                    val escape = escapeCode.toChar()
                    if (copy) output.write(escape)
                    val decodedChar = when (escape) {
                        '"', '\\', '/' -> escape
                        'b' -> '\b'
                        'f' -> '\u000C'
                        'n' -> '\n'
                        'r' -> '\r'
                        't' -> '\t'
                        'u' -> {
                            var number = 0
                            repeat(4) {
                                val hexCode = take()
                                val digit = if (hexCode < 0) null else hexCode.toChar().digitToIntOrNull(16)
                                require(digit != null) { "Invalid OpenCode export Unicode escape" }
                                if (copy) output.write(hexCode.toChar())
                                number = number * 16 + digit
                            }
                            number.toChar()
                        }
                        else -> throw java.io.IOException("Invalid OpenCode export escape")
                    }
                    keyCharacter(decodedChar)
                }
                else -> keyCharacter(char)
            }
        }
    }

    private fun number(copy: Boolean) {
        fun emit() { val char = take().toChar(); if (copy) output.write(char) }
        if (peek() == '-'.code) emit()
        if (peek() == '0'.code) emit()
        else {
            require(peek() in '1'.code..'9'.code) { "Invalid OpenCode export number" }
            do { emit() } while (peek() in '0'.code..'9'.code)
        }
        if (peek() == '.'.code) {
            emit()
            require(peek() in '0'.code..'9'.code) { "Invalid OpenCode export fraction" }
            do { emit() } while (peek() in '0'.code..'9'.code)
        }
        if (peek() == 'e'.code || peek() == 'E'.code) {
            emit()
            if (peek() == '+'.code || peek() == '-'.code) emit()
            require(peek() in '0'.code..'9'.code) { "Invalid OpenCode export exponent" }
            do { emit() } while (peek() in '0'.code..'9'.code)
        }
    }

    private fun value(copy: Boolean, depth: Int) {
        require(depth <= 128) { "OpenCode export nesting exceeds safe limits" }
        when (peek()) {
            '"'.code -> string(copy)
            '{'.code -> {
                expect('{', copy)
                whitespace(copy)
                if (peek() != '}'.code) {
                    while (true) {
                        string(copy, key = true)
                        whitespace(copy)
                        expect(':', copy)
                        whitespace(copy)
                        value(copy, depth + 1)
                        whitespace(copy)
                        if (peek() != ','.code) break
                        expect(',', copy)
                        whitespace(copy)
                    }
                }
                expect('}', copy)
            }
            '['.code -> {
                expect('[', copy)
                whitespace(copy)
                if (peek() != ']'.code) {
                    while (true) {
                        value(copy, depth + 1)
                        whitespace(copy)
                        if (peek() != ','.code) break
                        expect(',', copy)
                        whitespace(copy)
                    }
                }
                expect(']', copy)
            }
            't'.code -> "true".forEach { expect(it, copy) }
            'f'.code -> "false".forEach { expect(it, copy) }
            'n'.code -> "null".forEach { expect(it, copy) }
            else -> number(copy)
        }
    }
}

private class BufferedExportOutput(private val output: Writer) {
    private val buffer = CharArray(8192)
    private var count = 0
    fun write(char: Char) {
        buffer[count++] = char
        if (count == buffer.size) drain()
    }
    private fun drain() {
        if (count > 0) output.write(buffer, 0, count)
        count = 0
    }
    fun flush() { drain(); output.flush() }
}
