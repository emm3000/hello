package com.emm.hello.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.bricolage
import com.emm.hello.core.theme.destructiveInk
import com.emm.hello.core.theme.hairline
import com.emm.hello.core.theme.helloShapes
import com.emm.hello.core.theme.ink
import com.emm.hello.core.theme.inkFaint
import com.emm.hello.core.theme.onInk
import com.emm.hello.core.theme.outline
import com.emm.hello.core.theme.surfaceRaised

enum class HButtonVariant { Primary, Secondary, Text }

private val primaryLabelStyle = TextStyle(
    fontFamily = bricolage,
    fontWeight = FontWeight.Bold,
    fontSize = 18.sp,
    lineHeight = 20.sp,
    letterSpacing = (-0.02).em,
)

private val iconSize: Dp = 18.dp
private val iconGap: Dp = 8.dp
private val borderWidth: Dp = 1.dp

@Composable
fun HButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: HButtonVariant = HButtonVariant.Primary,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    danger: Boolean = false,
    full: Boolean = false,
    icon: ImageVector? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val anatomy: ButtonAnatomy = variant.anatomy()
    val interactive: Boolean = enabled && !isLoading
    val widthModifier: Modifier = if (full) modifier.fillMaxWidth() else modifier

    Button(
        onClick = onClick,
        modifier = widthModifier.heightIn(min = anatomy.minHeight),
        enabled = interactive,
        shape = MaterialTheme.helloShapes.pill,
        colors = anatomy.colors(danger),
        border = anatomy.border(interactive),
        contentPadding = PaddingValues(horizontal = anatomy.horizontalPadding),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            disabledElevation = 0.dp,
        ),
    ) {
        ButtonContent(icon = icon, isLoading = isLoading, content = content)
    }
}

@Composable
fun HButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: HButtonVariant = HButtonVariant.Primary,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    danger: Boolean = false,
    full: Boolean = false,
    icon: ImageVector? = null,
) {
    val labelStyle: TextStyle = when (variant) {
        HButtonVariant.Primary -> primaryLabelStyle
        HButtonVariant.Secondary, HButtonVariant.Text -> MaterialTheme.typography.titleSmall
    }
    HButton(
        onClick = onClick,
        modifier = modifier,
        variant = variant,
        enabled = enabled,
        isLoading = isLoading,
        danger = danger,
        full = full,
        icon = icon,
    ) {
        Text(
            text = if (isLoading) "Loading…" else text,
            style = labelStyle,
        )
    }
}

private data class ButtonAnatomy(
    val minHeight: Dp,
    val horizontalPadding: Dp,
    val filled: Boolean,
    val outlined: Boolean,
)

private val primaryAnatomy = ButtonAnatomy(
    minHeight = 60.dp,
    horizontalPadding = 28.dp,
    filled = true,
    outlined = false,
)

private val secondaryAnatomy = ButtonAnatomy(
    minHeight = 52.dp,
    horizontalPadding = 24.dp,
    filled = false,
    outlined = true,
)

private val textAnatomy = ButtonAnatomy(
    minHeight = 44.dp,
    horizontalPadding = 16.dp,
    filled = false,
    outlined = false,
)

private fun HButtonVariant.anatomy(): ButtonAnatomy = when (this) {
    HButtonVariant.Primary -> primaryAnatomy
    HButtonVariant.Secondary -> secondaryAnatomy
    HButtonVariant.Text -> textAnatomy
}

private fun ButtonAnatomy.colors(danger: Boolean): ButtonColors {
    val accent: Color = if (danger) destructiveInk else ink
    return if (filled) {
        ButtonColors(
            containerColor = accent,
            contentColor = onInk,
            disabledContainerColor = surfaceRaised,
            disabledContentColor = inkFaint,
        )
    } else {
        ButtonColors(
            containerColor = Color.Transparent,
            contentColor = accent,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = inkFaint,
        )
    }
}

private fun ButtonAnatomy.border(interactive: Boolean): BorderStroke? = when {
    !outlined -> null
    interactive -> BorderStroke(borderWidth, outline)
    else -> BorderStroke(borderWidth, hairline)
}

@Composable
private fun RowScope.ButtonContent(
    icon: ImageVector?,
    isLoading: Boolean,
    content: @Composable RowScope.() -> Unit,
) {
    when {
        isLoading -> {
            CircularProgressIndicator(
                modifier = Modifier.size(iconSize),
                color = LocalContentColor.current,
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.width(iconGap))
        }
        icon != null -> {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
            )
            Spacer(Modifier.width(iconGap))
        }
    }
    content()
}

@Composable
private fun PreviewPair(
    text: String,
    variant: HButtonVariant,
    danger: Boolean = false,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        HButton(
            text = text,
            onClick = {},
            variant = variant,
            danger = danger,
            modifier = Modifier.weight(1f),
        )
        HButton(
            text = text,
            onClick = {},
            variant = variant,
            danger = danger,
            enabled = false,
            modifier = Modifier.weight(1f),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF4F3F1)
@Composable
private fun HButtonAnatomyPreview() {
    HelloTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PreviewPair(text = "Knew it", variant = HButtonVariant.Primary)
            PreviewPair(text = "Forgot", variant = HButtonVariant.Secondary)
            PreviewPair(text = "Not now", variant = HButtonVariant.Text)
            PreviewPair(text = "Delete card", variant = HButtonVariant.Primary, danger = true)
            PreviewPair(text = "Delete card", variant = HButtonVariant.Secondary, danger = true)
            PreviewPair(text = "Delete card", variant = HButtonVariant.Text, danger = true)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF4F3F1)
@Composable
private fun HButtonStatesPreview() {
    HelloTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HButton(
                text = "Add a word",
                onClick = {},
                icon = Icons.Default.Add,
                full = true,
            )
            HButton(
                text = "Saving",
                onClick = {},
                isLoading = true,
                full = true,
            )
            HButton(
                text = "Add a word",
                onClick = {},
                variant = HButtonVariant.Secondary,
                icon = Icons.Default.Add,
                full = true,
            )
        }
    }
}
