package com.emm.hello.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.emm.hello.core.theme.HelloTheme
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class SharedControlsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun input_exposes_label_error_and_min_touch_target() {
        composeRule.setContent {
            HelloTheme {
                HInput(
                    value = "",
                    onValueChange = {},
                    label = "Nombre",
                    errorMessage = "Campo obligatorio",
                )
            }
        }

        composeRule.waitUntilAtLeastOneExists(
            hasSetTextAction()
                .and(hasContentDescription("Nombre"))
                .and(hasError("Campo obligatorio")),
        )
        composeRule.onNode(
            hasSetTextAction()
                .and(hasContentDescription("Nombre"))
                .and(hasError("Campo obligatorio")),
        )
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun warning_components_expose_warning_semantics() {
        composeRule.setContent {
            HelloTheme {
                Column {
                    HAlert(
                        title = "Atención",
                        description = "Necesita revisión",
                        variant = AlertVariant.Warning,
                    )
                }
            }
        }

        composeRule.waitUntilAtLeastOneExists(
            hasWarningState().and(hasAnyDescendant(hasText("Atención"))),
        )
    }

    @Test
    fun button_uses_min_touch_target() {
        composeRule.setContent {
            HelloTheme {
                HButton(
                    text = "Guardar",
                    onClick = {},
                )
            }
        }

        composeRule.waitUntilAtLeastOneExists(hasClickAction().and(hasText("Guardar")))
        composeRule.onNode(hasClickAction().and(hasText("Guardar")))
            .assertHeightIsAtLeast(48.dp)
    }
}

private fun hasError(message: String): SemanticsMatcher = SemanticsMatcher("Has error '$message'") { node ->
    node.config.getOrNull(SemanticsProperties.Error) == message
}

private fun hasWarningState(): SemanticsMatcher = SemanticsMatcher("Has warning state") { node ->
    node.config.getOrNull(SemanticsProperties.StateDescription) == "Advertencia"
}
