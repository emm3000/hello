package com.emm.hello.newfeatures.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.emm.hello.di.newModule
import com.emm.hello.di.testModule
import com.emm.hello.newfeatures.NewRoot
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class SettingsScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        stopKoin()
        startKoin {
            androidContext(composeRule.activity)
            modules(newModule, testModule)
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun settingsScreen_reachesResumedState() {
        composeRule.setContent {
            NewRoot()
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Settings").assertIsDisplayed()

        val activity = composeRule.activity
        assertEquals(
            "Settings screen should be in RESUMED state",
            Lifecycle.State.RESUMED,
            activity.lifecycle.currentState
        )
    }

    @Test
    fun settingsScreen_navigationFromHoy_toSettings_andBack() {
        composeRule.setContent {
            NewRoot()
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        composeRule.onNodeWithText("Backup").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Settings").assertDoesNotExist()
    }

    @Test
    fun settingsScreen_exportAndImportButtons_areVisible() {
        composeRule.setContent {
            NewRoot()
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Export data").assertIsDisplayed()
        composeRule.onNodeWithText("Restore from backup").assertIsDisplayed()
    }
}
