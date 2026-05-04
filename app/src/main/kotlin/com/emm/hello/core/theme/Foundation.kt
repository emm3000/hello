package com.emm.hello.core.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class HelloSpacing(
    val xs: Dp,
    val sm: Dp,
    val md: Dp,
    val lg: Dp,
    val xl: Dp,
    val xxl: Dp,
)

@Immutable
data class HelloSemanticColor(
    val container: Color,
    val content: Color,
    val accent: Color,
)

@Immutable
data class HelloSemanticColors(
    val success: HelloSemanticColor,
    val warning: HelloSemanticColor,
    val destructive: HelloSemanticColor,
)

@Immutable
data class HelloShapes(
    val control: RoundedCornerShape,
    val container: RoundedCornerShape,
    val pill: RoundedCornerShape,
)

internal val defaultHelloSpacing = HelloSpacing(
    xs = 4.dp,
    sm = 8.dp,
    md = 12.dp,
    lg = 16.dp,
    xl = 24.dp,
    xxl = 32.dp,
)

internal val defaultHelloShapes = HelloShapes(
    control = RoundedCornerShape(8.dp),
    container = RoundedCornerShape(12.dp),
    pill = RoundedCornerShape(100.dp),
)

internal val helloMaterialShapes = Shapes(
    small = defaultHelloShapes.control,
    medium = defaultHelloShapes.container,
    large = RoundedCornerShape(16.dp),
)

internal val localHelloSpacing = staticCompositionLocalOf { defaultHelloSpacing }
internal val localHelloSemanticColors = staticCompositionLocalOf { lightSemanticColors() }
internal val localHelloShapes = staticCompositionLocalOf { defaultHelloShapes }

val MaterialTheme.spacing: HelloSpacing
    @Composable
    @ReadOnlyComposable
    get() = localHelloSpacing.current

val MaterialTheme.semanticColors: HelloSemanticColors
    @Composable
    @ReadOnlyComposable
    get() = localHelloSemanticColors.current

val MaterialTheme.helloShapes: HelloShapes
    @Composable
    @ReadOnlyComposable
    get() = localHelloShapes.current

@Composable
internal fun ProvideHelloFoundations(
    semanticColors: HelloSemanticColors,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        localHelloSpacing provides defaultHelloSpacing,
        localHelloSemanticColors provides semanticColors,
        localHelloShapes provides defaultHelloShapes,
        content = content,
    )
}

@PreviewLightDark
@Composable
private fun HelloFoundationPreview() {
    HelloTheme {
        Surface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
            ) {
                Text(
                    text = "Foundation accessors",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Metadata text now uses a named typography role.",
                    style = MaterialTheme.typography.metadata,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
                    SemanticSwatch("Success", MaterialTheme.semanticColors.success)
                    SemanticSwatch("Warning", MaterialTheme.semanticColors.warning)
                    SemanticSwatch("Destructive", MaterialTheme.semanticColors.destructive)
                }
                Box(
                    modifier = Modifier
                        .size(width = 120.dp, height = 40.dp)
                        .clip(MaterialTheme.helloShapes.pill)
                        .background(MaterialTheme.semanticColors.success.container),
                )
            }
        }
    }
}

@Composable
private fun SemanticSwatch(label: String, colors: HelloSemanticColor) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
        Box(
            modifier = Modifier
                .size(72.dp, 40.dp)
                .clip(MaterialTheme.helloShapes.container)
                .background(colors.container),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.metadata,
            color = colors.content,
        )
    }
}
