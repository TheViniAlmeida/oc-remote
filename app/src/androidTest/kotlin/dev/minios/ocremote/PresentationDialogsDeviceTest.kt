package dev.minios.ocremote

import android.app.KeyguardManager
import android.content.Context
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.minios.ocremote.data.api.AgentInfo
import dev.minios.ocremote.domain.export.SessionExportFormat
import dev.minios.ocremote.domain.model.*
import dev.minios.ocremote.ui.components.AppearanceSettingsDialog
import dev.minios.ocremote.ui.components.NotificationPreferencesDialog
import dev.minios.ocremote.ui.components.TimelineSettingsDialog
import dev.minios.ocremote.ui.screens.chat.AgentPickerDialog
import dev.minios.ocremote.ui.screens.chat.SessionDiffDialog
import dev.minios.ocremote.ui.screens.chat.SessionDiffState
import dev.minios.ocremote.ui.screens.chat.SessionExportDialog
import dev.minios.ocremote.ui.screens.home.ServerDialog
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeFalse
import org.junit.AssumptionViolatedException
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import java.util.concurrent.CancellationException

/**
 * Real-device Compose interactions with synthetic, in-memory callbacks only.
 * Uses ComponentActivity, never MainActivity, repositories, network, clipboard or system settings.
 * Failures expose a fixed stage only: no semantics dump, screenshot, exception cause or fixture text.
 */
@RunWith(AndroidJUnit4::class)
class PresentationDialogsDeviceTest {
    private val compose = createAndroidComposeRule<ComponentActivity>()
    @Volatile private var phase = "activity-lifecycle"
    private val sanitizedFailures = object : TestRule {
        override fun apply(base: Statement, description: Description): Statement = object : Statement() {
            override fun evaluate() {
                try {
                    val context = InstrumentationRegistry.getInstrumentation().targetContext
                    val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                    assumeFalse("Unlock the device before running synthetic dialog checks", keyguard.isKeyguardLocked)
                    base.evaluate()
                } catch (failure: AssumptionViolatedException) {
                    throw failure
                } catch (_: CancellationException) {
                    throw CancellationException("Synthetic dialog checks cancelled")
                } catch (_: Throwable) {
                    throw AssertionError("Synthetic presentation dialog check failed at stage: $phase")
                }
            }
        }
    }
    @get:Rule val rules: RuleChain = RuleChain.outerRule(sanitizedFailures).around(compose)

    @Before fun keepTestWindowAwake() {
        compose.activityRule.scenario.onActivity { it.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    @After fun restoreTestWindow() {
        compose.activityRule.scenario.onActivity { it.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    private fun content(block: @Composable () -> Unit) { compose.setContent { MaterialTheme { block() } } }
    private fun label(id: Int) = compose.activity.getString(id)
    private fun clickScrolling(id: Int) = compose.onNodeWithText(label(id)).performScrollTo().performClick()
    private fun toggle(index: Int) = compose.onAllNodes(isToggleable())[index].performScrollTo().performClick()

    @Test fun agentSearchExcludesHiddenAndSubagentsAndScrollsToSelectableAgent() {
        phase = "agent-picker-search"
        var selected: String? = null
        val agents = (0..39).map { AgentInfo("Synthetic Agent ${it.toString().padStart(2, '0')}") } + listOf(
            AgentInfo("Synthetic Hidden", hidden = true), AgentInfo("Synthetic Worker", mode = "subagent"),
            AgentInfo("Synthetic Both", mode = "all", description = "Research fixture"),
        )
        content { AgentPickerDialog(agents, "Synthetic Agent 00", { selected = it }, {}) }
        val search = compose.onNode(hasSetTextAction())
        search.performTextReplacement("Hidden")
        compose.onNodeWithText(label(R.string.agent_catalog_empty)).assertIsDisplayed()
        search.performTextReplacement("Worker")
        compose.onNodeWithText(label(R.string.agent_catalog_empty)).assertIsDisplayed()
        search.performTextReplacement("Research")
        compose.onNodeWithText("Synthetic Both").assertIsDisplayed()
        search.performTextReplacement("")
        phase = "agent-picker-scroll-selection"
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Synthetic Agent 39"))
        compose.onNodeWithText("Synthetic Agent 39").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("Synthetic Agent 39", selected) }
    }

    @Test fun appearanceCancelDoesNotApplyDraft() {
        phase = "appearance-cancel"
        var saves = 0
        var dismissals = 0
        content { AppearanceSettingsDialog("Synthetic appearance", null, ThemeAppearance(), { saves++ }, { dismissals++ }) }
        clickScrolling(R.string.appearance_dark)
        clickScrolling(R.string.appearance_blue)
        clickScrolling(R.string.appearance_cancel)
        compose.runOnIdle { assertEquals(0, saves); assertEquals(1, dismissals) }
    }

    @Test fun appearanceSaveAppliesThemeAndAccentOnce() {
        phase = "appearance-save"
        var saved: AppearanceOverride? = null
        var saves = 0
        content { AppearanceSettingsDialog("Synthetic appearance", null, ThemeAppearance(), { saved = it; saves++ }, {}) }
        clickScrolling(R.string.appearance_dark)
        clickScrolling(R.string.appearance_blue)
        compose.runOnIdle { assertEquals(0, saves) }
        clickScrolling(R.string.appearance_save)
        compose.runOnIdle { assertEquals(1, saves); assertEquals("dark", saved?.theme); assertEquals("blue", saved?.accent) }
    }

    @Test fun appearanceRestoreRemovesScopedOverride() {
        phase = "appearance-restore"
        var saved: AppearanceOverride? = AppearanceOverride(theme = "dark")
        var saves = 0
        content { AppearanceSettingsDialog("Synthetic appearance", saved, ThemeAppearance(), { saved = it; saves++ }, {}) }
        clickScrolling(R.string.appearance_reset)
        compose.runOnIdle { assertEquals(1, saves); assertNull(saved) }
    }

    @Test fun timelinePresetAndCustomActivityRequireExplicitSave() {
        phase = "timeline-preset-custom-save"
        var saved: TimelinePreferences? = null
        var saves = 0
        content { TimelineSettingsDialog("Synthetic timeline", TimelinePreferences(), onSave = { saved = it; saves++ }, onDismiss = {}) }
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
            .performSemanticsAction(SemanticsActions.SetProgress) { it(3f) }
        compose.onNodeWithText(label(R.string.timeline_detailed)).assertExists()
        toggle(1) // Shell visibility after the inheritance switch.
        toggle(2) // Shell grouping.
        toggle(3) // Shell collapse.
        compose.runOnIdle { assertEquals(0, saves) }
        clickScrolling(R.string.timeline_save)
        compose.runOnIdle {
            assertEquals(1, saves)
            assertEquals(TimelinePreset.CUSTOM, saved?.preset)
            assertEquals(TimelineActivity(visible = false, grouped = true, collapsed = true), saved?.shell)
        }
    }

    @Test fun timelineCancelDiscardsPresetChange() {
        phase = "timeline-cancel"
        var saves = 0
        var dismissals = 0
        content { TimelineSettingsDialog("Synthetic timeline", TimelinePreferences(), onSave = { saves++ }, onDismiss = { dismissals++ }) }
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0f) }
        clickScrolling(R.string.timeline_cancel)
        compose.runOnIdle { assertEquals(0, saves); assertEquals(1, dismissals) }
    }

    @Test fun timelineInheritanceSavesNullInsteadOfCopyingParent() {
        phase = "timeline-inheritance"
        var saved: TimelinePreferences? = TimelinePreferences()
        var saves = 0
        content { TimelineSettingsDialog("Synthetic timeline", saved, onSave = { saved = it; saves++ }, onDismiss = {}) }
        toggle(0)
        clickScrolling(R.string.timeline_save)
        compose.runOnIdle { assertEquals(1, saves); assertNull(saved) }
    }

    @Test fun notificationDraftSelectsCategorySoundTimingAndSavesOnlyOnConfirmation() {
        phase = "notification-category-save"
        var saved: NotificationPreferences? = null
        var saves = 0
        content { NotificationPreferencesDialog(NotificationPreferences(), inherited = true, allowInheritance = true, onSave = { saved = it; saves++ }, onDismiss = {}) }
        toggle(0) // Make an explicit scoped override.
        compose.onAllNodesWithText(label(R.string.notification_preferences_default_sound))[0].performScrollTo().performClick()
        compose.onNodeWithText(label(R.string.notification_preferences_silent)).performClick()
        toggle(2) // Agent vibration off.
        toggle(1) // Agent notifications off.
        compose.onAllNodesWithText(label(R.string.notification_preferences_outside_session))[1].performScrollTo().performClick()
        compose.onNodeWithText(label(R.string.notification_preferences_always)).performClick()
        compose.runOnIdle { assertEquals(0, saves) }
        clickScrolling(R.string.notification_preferences_save)
        compose.runOnIdle {
            assertEquals(1, saves)
            assertFalse(saved!!.agent.enabled)
            assertFalse(saved!!.agent.vibrate)
            assertEquals(NotificationSound.SILENT, saved!!.agent.sound)
            assertEquals(NotificationTiming.ALWAYS, saved!!.permission.timing)
        }
    }

    @Test fun notificationCancelDiscardsDraftWithoutCreatingSystemChannels() {
        phase = "notification-cancel"
        var saves = 0
        var dismissals = 0
        content { NotificationPreferencesDialog(NotificationPreferences(), allowInheritance = true, onSave = { saves++ }, onDismiss = { dismissals++ }) }
        toggle(1)
        clickScrolling(R.string.notification_preferences_cancel)
        compose.runOnIdle { assertEquals(0, saves); assertEquals(1, dismissals) }
    }

    @Test fun notificationInheritanceRemovesSessionOverride() {
        phase = "notification-inheritance"
        var saves = 0
        var saved: NotificationPreferences? = NotificationPreferences()
        content { NotificationPreferencesDialog(NotificationPreferences(), allowInheritance = true, onSave = { saved = it; saves++ }, onDismiss = {}) }
        toggle(0)
        clickScrolling(R.string.notification_preferences_save)
        compose.runOnIdle { assertEquals(1, saves); assertNull(saved) }
    }

    @Test fun exportSelectionDispatchesCopyAndDownloadWithoutExternalEffects() {
        phase = "export-format-callbacks"
        val copied = mutableListOf<SessionExportFormat>()
        val downloaded = mutableListOf<SessionExportFormat>()
        content { SessionExportDialog({ copied += it }, { downloaded += it }, {}) }
        compose.onNodeWithText(label(R.string.session_export_copy)).performClick()
        compose.onNodeWithText("JSON (.json)").performClick()
        compose.onNodeWithText(label(R.string.session_export_copy)).performClick()
        compose.onNodeWithText("HTML (.html)").performClick()
        compose.onNodeWithText(label(R.string.session_export_download)).performClick()
        compose.runOnIdle {
            assertEquals(listOf(SessionExportFormat.MARKDOWN, SessionExportFormat.JSON), copied)
            assertEquals(listOf(SessionExportFormat.HTML), downloaded)
        }
    }

    @Test fun exportCancelDoesNotCopyOrDownload() {
        phase = "export-cancel"
        var copies = 0
        var downloads = 0
        var dismissals = 0
        content { SessionExportDialog({ copies++ }, { downloads++ }, { dismissals++ }) }
        compose.onNodeWithText("HTML (.html)").performClick()
        compose.onNodeWithText(label(R.string.server_cancel)).performClick()
        compose.runOnIdle { assertEquals(0, copies); assertEquals(0, downloads); assertEquals(1, dismissals) }
    }

    @Test fun diffExpandsBeforeAndAfterContentAndCollapsesAgain() {
        phase = "session-diff-expansion"
        val diff = FileDiff("synthetic.txt", before = "SYNTHETIC BEFORE", after = "SYNTHETIC AFTER", additions = 1, deletions = 1)
        content { SessionDiffDialog(SessionDiffState(files = listOf(diff)), {}, {}) }
        compose.onNodeWithText("SYNTHETIC BEFORE").assertDoesNotExist()
        compose.onNodeWithText("synthetic.txt", substring = true).performClick()
        compose.onNodeWithText("SYNTHETIC BEFORE").assertIsDisplayed()
        compose.onNodeWithText("SYNTHETIC AFTER").assertIsDisplayed()
        compose.onNodeWithText("synthetic.txt", substring = true).performClick()
        compose.onNodeWithText("SYNTHETIC BEFORE").assertDoesNotExist()
    }

    @Test fun serverPasswordEyeChangesVisibilityWithoutSavingSyntheticConfiguration() {
        phase = "server-password-eye"
        var saves = 0
        val server = ServerConfig(id = "synthetic-server", url = "https://example.invalid", password = "SYNTHETIC_PASSWORD", autoConnect = false)
        content { ServerDialog(server, {}, { _, _, _, _, _ -> saves++ }) }
        val field = hasSetTextAction()
        compose.onNode(field and SemanticsMatcher.keyIsDefined(SemanticsProperties.Password)).assertExists()
        compose.onNodeWithContentDescription(label(R.string.server_password_show)).performScrollTo().performClick()
        compose.onNode(field and SemanticsMatcher.keyIsDefined(SemanticsProperties.Password)).assertDoesNotExist()
        compose.onAllNodes(field).assertCountEquals(4)
        compose.onNodeWithContentDescription(label(R.string.server_password_hide)).performScrollTo().performClick()
        compose.onNode(field and SemanticsMatcher.keyIsDefined(SemanticsProperties.Password)).assertExists()
        compose.onNodeWithText(label(R.string.server_cancel)).performClick()
        compose.runOnIdle { assertEquals(0, saves) }
    }
}
