package com.emm.hello.newfeatures.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.emm.hello.newfeatures.NewRoot
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class SettingsScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun settingsScreen_reachesResumedState() {
        composeRule.setContent {
            NewRoot()
        }

        // Wait for startup to complete and dashboard to load
        composeRule.waitForIdle()

        // Navigate to Settings screen via the settings icon in TopAppBar
        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.waitForIdle()

        // Verify Settings screen is displayed by checking the title
        composeRule.onNodeWithText("Settings").assertIsDisplayed()

        // Verify lifecycle state is RESUMED
        val activity = composeRule.activity
        assertEquals(
            "Settings screen should be in RESUMED state",
            Lifecycle.State.RESUMED,
            activity.lifecycle.currentState
        )
    }

    @Test
    fun settingsScreen_navigationFromDashboard_toSettings_andBack() {
        composeRule.setContent {
            NewRoot()
        }

        composeRule.waitForIdle()

        // Navigate to Settings
        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.waitForIdle()

        // Verify Settings screen content
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        composeRule.onNodeWithText("Backup").assertIsDisplayed()

        // Navigate back using the back arrow
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitForIdle()

        // Verify we're back on Dashboard (no "Settings" title visible)
        composeRule.onNodeWithText("Settings").assertDoesNotExist()
    }

    @Test
    fun settingsScreen_exportAndImportButtons_areVisible() {
        composeRule.setContent {
            NewRoot()
        }

        composeRule.waitForIdle()

        // Navigate to Settings
        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.waitForIdle()

        // Verify export and import buttons are displayed
        composeRule.onNodeWithText("Export data").assertIsDisplayed()
        composeRule.onNodeWithText("Restore backup").assertIsDisplayed()
    }
}