package com.emm.hello.core.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.lightSemanticColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RefineDesignSystemRuntimeProofTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun stat_card_consumes_shared_theme_foundations() {
        val expectedContainer = lightSemanticColors().success.container

        composeRule.setContent {
            HelloTheme(darkTheme = false) {
                Box(
                    modifier = Modifier
                        .background(Color.Magenta)
                        .padding(12.dp)
                ) {
                    StatCard(
                        value = "24",
                        label = "tarjetas",
                        status = StatCardStatus.Success,
                        modifier = Modifier.testTag("stat-card"),
                    )
                }
            }
        }

        composeRule.onNodeWithText("24").fetchSemanticsNode()
        composeRule.onNodeWithText("tarjetas").fetchSemanticsNode()

        val pixelMap = composeRule.onNodeWithTag("stat-card")
            .captureToImage()
            .toPixelMap()

        val centerColor = pixelMap[pixelMap.width / 2, pixelMap.height / 2]
        val topLeftColor = pixelMap[0, 0]

        assertEquals(expectedContainer.toArgb(), centerColor.toArgb())
        assertNotEquals(expectedContainer.toArgb(), topLeftColor.toArgb())
    }
}
