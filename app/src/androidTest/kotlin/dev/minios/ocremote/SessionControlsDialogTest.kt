package dev.minios.ocremote

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.minios.ocremote.domain.model.SessionControls
import dev.minios.ocremote.ui.screens.chat.SessionControlsDialog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises draft/save behavior with synthetic state; never accesses servers or preferences. */
@RunWith(AndroidJUnit4::class)
class SessionControlsDialogTest {
    @get:Rule val compose = createComposeRule()

    @Before
    fun requiresUnlockedDisplay() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val keyguard = context.getSystemService(android.content.Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
        assumeFalse("Unlock the device before running dialog interaction tests", keyguard.isKeyguardLocked)
    }

    @Test
    fun cancelledDraftDoesNotEnableAutomaticActions() {
        var saved: SessionControls? = null
        var dismissals = 0
        compose.setContent {
            MaterialTheme {
                SessionControlsDialog(SessionControls(), { saved = it }, { dismissals++ })
            }
        }
        for (index in 0..2) compose.onAllNodes(isToggleable())[index].assertIsOff()
        compose.onAllNodes(isToggleable())[0].performScrollTo().performClick()
        compose.onNodeWithText(label(R.string.session_controls_cancel)).performScrollTo().performClick()
        compose.runOnIdle {
            assertNull(saved)
            assertEquals(1, dismissals)
        }
    }

    @Test
    fun explicitSaveAppliesControlsAndDefaultThreshold() {
        var saved: SessionControls? = null
        var saves = 0
        compose.setContent {
            MaterialTheme {
                SessionControlsDialog(SessionControls(), { saved = it; saves++ }, {})
            }
        }
        for (index in 0..2) compose.onAllNodes(isToggleable())[index].performScrollTo().performClick()
        compose.runOnIdle { assertNull(saved) }
        compose.onNodeWithText(label(R.string.session_controls_save)).performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(1, saves)
            assertTrue(saved!!.autoApprovePermissions)
            assertTrue(saved!!.autoCompact)
            assertTrue(saved!!.muted)
            assertEquals(85, saved!!.compactionThresholdPercent)
        }
    }

    private fun label(id: Int): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)
}
