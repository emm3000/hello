package com.emm.hello.smoke

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.waitUntil
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.emm.hello.di.KoinComposeTestRule
import com.emm.hello.di.newModule
import com.emm.hello.di.repositoryModule
import com.emm.hello.di.testModule
import com.emm.hello.newfeatures.NewRoot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class NavigationSmokeTest {

    @get:Rule(order = 0)
    val koinRule = KoinComposeTestRule(newModule, repositoryModule, testModule)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun smokeTest_dashboardToSettingsAndBack() {
        composeRule.setContent {
            NewRoot()
        }

        // Wait for startup to complete and dashboard title to appear
        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithText("Mis mazos").fetchSemanticsNodes().isNotEmpty()
        }

        // Dashboard is visible
        composeRule.onNodeWithText("Mis mazos").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Settings").assertIsDisplayed()

        // Navigate to Settings
        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.waitForIdle()

        // Settings screen is visible
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        composeRule.onNodeWithText("Backup").assertIsDisplayed()
        composeRule.onNodeWithText("Export data").assertIsDisplayed()
        composeRule.onNodeWithText("Restore backup").assertIsDisplayed()

        // Navigate back
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitForIdle()

        // Dashboard is visible again
        composeRule.onNodeWithText("Mis mazos").assertIsDisplayed()
        composeRule.onNodeWithText("Settings").assertDoesNotExist()
    }
}
