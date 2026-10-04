package dev.minios.ocremote.data.repository

import dev.minios.ocremote.domain.model.ServerConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ServerDuplicateDraftTest {
    @Test
    fun `duplicate has independent identity and no runtime or automatic connection state`() {
        val source = ServerConfig(
            id = "original",
            url = "https://example.com:4848",
            name = "Example",
            username = "YOUR_USERNAME_HERE",
            password = "YOUR_PASSWORD_HERE",
            autoConnect = true,
            lastConnected = 123L,
            isHealthy = true,
        )

        val duplicate = duplicateServerDraft(source)

        assertNotEquals(source.id, duplicate.id)
        assertEquals(duplicate.id, UUID.fromString(duplicate.id).toString())
        assertEquals(source.url, duplicate.url)
        assertEquals(source.name, duplicate.name)
        assertEquals(source.username, duplicate.username)
        assertEquals(source.password, duplicate.password)
        assertFalse(duplicate.autoConnect)
        assertFalse(duplicate.isHealthy)
        assertNull(duplicate.lastConnected)
        assertTrue(source.autoConnect)
        assertTrue(source.isHealthy)
        assertEquals(123L, source.lastConnected)
    }

    @Test
    fun `editing a duplicate endpoint leaves original and other duplicates independent`() {
        val source = ServerConfig(id = "original", url = "http://example.com:4096")
        val first = duplicateServerDraft(source).copy(url = "http://example.com:4097")
        val second = duplicateServerDraft(source)

        assertNotEquals(first.id, second.id)
        assertEquals(4096, source.port)
        assertEquals(4096, second.port)
        assertEquals(4097, first.port)
        assertEquals("http://example.com:4096", source.url)
    }
}
