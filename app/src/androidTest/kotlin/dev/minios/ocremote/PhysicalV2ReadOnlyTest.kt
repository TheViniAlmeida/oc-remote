package dev.minios.ocremote

import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.minios.ocremote.data.api.*
import dev.minios.ocremote.data.repository.SettingsRepository
import dev.minios.ocremote.di.NetworkModule
import dev.minios.ocremote.domain.export.SessionExportFormat
import dev.minios.ocremote.domain.export.renderSessionExport
import dev.minios.ocremote.domain.export.validateNativeSessionExport
import dev.minios.ocremote.domain.model.ServerConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.AssumptionViolatedException
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.Writer

/** Opt-in GET-only checks. Authentication and server contents never leave the target app. */
@RunWith(AndroidJUnit4::class)
class PhysicalV2ReadOnlyTest {
    private var stage = "setup"

    @Test
    fun readsAuthenticatedCatalogsWithoutChangingServerState() = withConnection { api, _, conn, _ ->
        stage = "health"
        assertTrue(api.getHealth(conn).healthy)
        assertEquals(ServerProtocol.V2, ServerProtocolRegistry.knownProtocol(conn))
        stage = "model-catalog"
        val providers = api.getProviders(conn).providers
        val modelCount = providers.sumOf { it.models.size }
        assertTrue(modelCount > 0)
        stage = "agent-catalog"
        val agents = api.listAgents(conn)
        assertTrue(agents.isNotEmpty())
        assertTrue(agents.all { it.name.isNotBlank() })
        stage = "project-catalog"
        val projects = api.listProjects(conn)
        report("catalogsRead=true providers=${providers.size} models=$modelCount primaryAgents=${agents.count { !it.hidden && it.mode in setOf("primary", "all") }} subagents=${agents.count { it.mode in setOf("subagent", "all") }} projects=${projects.size}")
    }

    @Test
    fun exportsAndReadsDiffOfExistingOwnedSmokeSessionWithoutSending() = withConnection { api, v2, conn, cache ->
        stage = "locate-owned-session"
        val tempDirectory = v2.payload(conn, "/api/info").jsonObject["paths"]!!.jsonObject["tmp"]!!.jsonPrimitive.content
        val session = api.listSessions(conn, tempDirectory)
            .filter { it.title == "OC Remote V2 compatibility smoke test" && it.directory == tempDirectory }
            .maxByOrNull { it.time.created }
        assumeTrue("No existing owned smoke session; no session will be created", session != null)
        val owned = session!!
        stage = "read-owned-history"
        val history = api.listMessages(conn, owned.id, limit = 10, directory = owned.directory)
        assertTrue(history.isNotEmpty())
        stage = "native-export"
        val file = File.createTempFile("readonly-device-export-", ".json", cache)
        try {
            file.outputStream().use { api.exportSessionToStream(conn, owned.id, it, directory = owned.directory) }
            file.reader(Charsets.UTF_8).use(::validateNativeSessionExport)
            assertTrue(file.length() > 0)
            for (format in listOf(SessionExportFormat.MARKDOWN, SessionExportFormat.HTML)) {
                stage = "readable-export-${format.name.lowercase()}"
                val output = CountingWriter()
                file.reader(Charsets.UTF_8).use { renderSessionExport(it, output, format, "Owned synthetic smoke session") }
                assertTrue(output.count > 0)
            }
            stage = "owned-session-diff"
            val diff = api.getSessionDiff(conn, owned.id, owned.directory)
            report("ownedHistoryRead=true messages=${history.size} nativeJsonValidated=true markdownRendered=true htmlRendered=true diffRead=true changedFiles=${diff.size}")
        } finally {
            // Only this test's newly created file in the app's private runtime cache is removed.
            file.delete()
        }
    }

    private fun withConnection(block: suspend (OpenCodeApi, OpenCodeV2Api, ServerConnection, File) -> Unit) = runBlocking {
        val url = InstrumentationRegistry.getArguments().getString("serverUrl")
        assumeTrue("Live read-only checks require an explicit serverUrl argument", !url.isNullOrBlank())
        try {
            withTimeout(150_000) {
                val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
                val json = NetworkModule.provideJson()
                val store = NetworkModule.provideDataStore(context)
                val encoded = store.data.first()[stringPreferencesKey("servers")] ?: error("No configuration")
                val server = json.decodeFromString<List<ServerConfig>>(encoded).singleOrNull {
                    it.url.trimEnd('/') == url!!.trimEnd('/')
                } ?: error("No matching configured server")
                val conn = ServerConnection.from(server.url, server.username, server.password)
                val client = NetworkModule.provideHttpClient(json)
                try {
                    block(OpenCodeApi(client, json, SettingsRepository(store, json, context), MessageImageCache(context)),
                        OpenCodeV2Api(client, json), conn, context.cacheDir)
                } finally { client.close() }
            }
        } catch (skipped: AssumptionViolatedException) {
            throw skipped
        } catch (_: Throwable) {
            throw AssertionError("Physical read-only V2 check failed at stage: $stage")
        }
    }

    private fun report(value: String) {
        InstrumentationRegistry.getInstrumentation().sendStatus(100, android.os.Bundle().apply {
            putString("ocremoteReadOnly", value)
        })
    }

    private class CountingWriter : Writer() {
        var count = 0L
        override fun write(buffer: CharArray, offset: Int, length: Int) { count += length }
        override fun flush() = Unit
        override fun close() = Unit
    }
}
