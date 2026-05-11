package com.emm.hello.smoke

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.emm.hello.di.newModule
import com.emm.hello.di.testModule
import com.emm.hello.newfeatures.NewRoot
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class NavigationSmokeTest {

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
    fun smokeTest_dashboardToSettingsAndBack() {
        composeRule.setContent {
            NewRoot()
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText("Mis mazos").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Settings").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        composeRule.onNodeWithText("Backup").assertIsDisplayed()
        composeRule.onNodeWithText("Export data").assertIsDisplayed()
        composeRule.onNodeWithText("Restore from backup").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Mis mazos").assertIsDisplayed()
        composeRule.onNodeWithText("Settings").assertDoesNotExist()
    }
}
