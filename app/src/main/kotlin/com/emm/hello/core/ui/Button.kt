package com.emm.hello.core.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.emberAccent
import com.emm.hello.core.theme.emberBad
import com.emm.hello.core.theme.emberBg
import com.emm.hello.core.theme.emberElev
import com.emm.hello.core.theme.emberMuted
import com.emm.hello.core.theme.emberPrimary
import com.emm.hello.core.theme.geist

// ── Legacy variants kept for backwards compatibility with feature screens ──
enum class ButtonVariant { Default, Destructive, Outline, Secondary, Ghost, Link }

// ── Ember design variants ──────────────────────────────────────────────────
enum class HButtonVariant { Primary, Accent, Secondary, Ghost }

enum class HButtonSize(val heightDp: Dp, val horizontalPadding: Dp, val fontSize: Int) {
    Sm(36.dp, 14.dp, 13),
    Md(48.dp, 22.dp, 15),
    Lg(56.dp, 28.dp, 16),
}

private const val DISABLED_ALPHA = 0.38f
private val emberButtonFg = Color(0xFF15110D)

/**
 * Ember-styled HButton. Pill radius (height/2), variants primary/accent/secondary/ghost,
 * sizes sm/md/lg, optional leading icon, optional full-width, optional danger flag.
 *
 * The old [ButtonVariant]-based overloads are kept below for backwards compatibility.
 */
@Composable
fun HButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: HButtonVariant = HButtonVariant.Primary,
    size: HButtonSize = HButtonSize.Md,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    danger: Boolean = false,
    full: Boolean = false,
    icon: ImageVector? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val height = size.heightDp
    val radius = height / 2
    val shape = RoundedCornerShape(radius)

    val (containerColor, contentColor, borderStroke) = emberButtonTokens(variant, danger)
    val buttonEnabled = enabled && !isLoading

    val disabledContainer = containerColor.copy(alpha = DISABLED_ALPHA)
    val disabledContent = contentColor.copy(alpha = DISABLED_ALPHA)

    val resolvedModifier = if (full) modifier.fillMaxWidth() else modifier

    Button(
        onClick = onClick,
        modifier = resolvedModifier.height(height),
        enabled = buttonEnabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainer,
            disabledContentColor = disabledContent,
        ),
        border = borderStroke,
        contentPadding = PaddingValues(horizontal = size.horizontalPadding),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
    ) {
        EmberButtonContent(icon = icon, isLoading = isLoading, content = content)
    }
}

@Composable
fun HButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: HButtonVariant = HButtonVariant.Primary,
    size: HButtonSize = HButtonSize.Md,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    danger: Boolean = false,
    full: Boolean = false,
    icon: ImageVector? = null,
) {
    HButton(
        onClick = onClick,
        modifier = modifier,
        variant = variant,
        size = size,
        enabled = enabled,
        isLoading = isLoading,
        danger = danger,
        full = full,
        icon = icon,
    ) {
        Text(
            text = if (isLoading) "Cargando…" else text,
            fontFamily = geist,
            fontWeight = FontWeight.Medium,
            fontSize = size.fontSize.sp,
            letterSpacing = (-0.1).sp,
        )
    }
}

// ── Legacy overloads — keep feature screens compiling unchanged ────────────

/**
 * Legacy lambda overload. Maps old [ButtonVariant] to the new Ember system.
 */
@Composable
fun HButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Default,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
    @Suppress("UNUSED_PARAMETER") contentPadding: PaddingValues? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val emberVariant = variant.toEmberVariant()
    val isDanger = variant == ButtonVariant.Destructive
    HButton(
        onClick = onClick,
        modifier = modifier,
        variant = emberVariant,
        size = HButtonSize.Md,
        enabled = enabled,
        isLoading = isLoading,
        danger = isDanger,
        full = false,
        icon = leadingIcon,
        content = content,
    )
}

/** Legacy text overload. */
@Composable
fun HButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Default,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
) {
    val emberVariant = variant.toEmberVariant()
    val isDanger = variant == ButtonVariant.Destructive
    HButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        variant = emberVariant,
        size = HButtonSize.Md,
        enabled = enabled,
        isLoading = isLoading,
        danger = isDanger,
        full = false,
        icon = leadingIcon,
    )
}

private fun ButtonVariant.toEmberVariant(): HButtonVariant = when (this) {
    ButtonVariant.Default -> HButtonVariant.Primary
    ButtonVariant.Destructive -> HButtonVariant.Primary // danger=true handles color
    ButtonVariant.Outline -> HButtonVariant.Secondary
    ButtonVariant.Secondary -> HButtonVariant.Secondary
    ButtonVariant.Ghost -> HButtonVariant.Ghost
    ButtonVariant.Link -> HButtonVariant.Ghost
}

// ── Internal helpers ───────────────────────────────────────────────────────

private data class ButtonTokens(
    val containerColor: Color,
    val contentColor: Color,
    val border: androidx.compose.foundation.BorderStroke?,
)

@Composable
private fun emberButtonTokens(
    variant: HButtonVariant,
    danger: Boolean,
): ButtonTokens = when (variant) {
    HButtonVariant.Primary -> ButtonTokens(
        containerColor = if (danger) emberBad else emberPrimary,
        contentColor = if (danger) Color.White else emberBg,
        border = null,
    )
    HButtonVariant.Accent -> ButtonTokens(
        containerColor = emberAccent,
        contentColor = emberButtonFg,
        border = null,
    )
    HButtonVariant.Secondary -> ButtonTokens(
        containerColor = Color.Transparent,
        contentColor = if (danger) emberBad else emberPrimary,
        border = androidx.compose.foundation.BorderStroke(1.dp, emberElev),
    )
    HButtonVariant.Ghost -> ButtonTokens(
        containerColor = Color.Transparent,
        contentColor = if (danger) emberBad else emberMuted,
        border = null,
    )
}

@Composable
private fun RowScope.EmberButtonContent(
    icon: ImageVector?,
    isLoading: Boolean,
    content: @Composable RowScope.() -> Unit,
) {
    if (isLoading) {
        val alpha by animateFloatAsState(
            targetValue = 1f,
            label = "loading_alpha",
        )
        CircularProgressIndicator(
            modifier = Modifier
                .size(16.dp)
                .graphicsLayer { this.alpha = alpha },
            color = LocalContentColor.current,
            strokeWidth = 2.dp,
        )
        Row(modifier = Modifier.width(8.dp)) {}
    } else if (icon != null) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Row(modifier = Modifier.width(8.dp)) {}
    }
    content()
}

// ── Previews ───────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun HButtonEmberVariantsPreview() {
    HelloTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HButton(text = "Estudiar ahora", onClick = {}, variant = HButtonVariant.Primary, full = true)
            HButton(text = "Comenzar", onClick = {}, variant = HButtonVariant.Accent, full = true)
            HButton(text = "Ver mazos", onClick = {}, variant = HButtonVariant.Secondary, full = true)
            HButton(text = "Cancelar", onClick = {}, variant = HButtonVariant.Ghost, full = true)
            HButton(text = "Borrar tarjeta", onClick = {}, variant = HButtonVariant.Ghost, danger = true, full = true)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun HButtonSizesPreview() {
    HelloTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HButton(text = "Pequeño", onClick = {}, variant = HButtonVariant.Accent, size = HButtonSize.Sm)
            HButton(text = "Mediano", onClick = {}, variant = HButtonVariant.Accent, size = HButtonSize.Md)
            HButton(text = "Grande", onClick = {}, variant = HButtonVariant.Accent, size = HButtonSize.Lg)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun HButtonWithIconPreview() {
    HelloTheme {
        Surface(color = emberBg) {
            HButton(
                text = "Nueva tarjeta",
                onClick = {},
                variant = HButtonVariant.Accent,
                icon = Icons.Default.Add,
                full = true,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun HButtonLoadingPreview() {
    HelloTheme {
        Surface(color = emberBg) {
            HButton(
                text = "Generar",
                onClick = {},
                variant = HButtonVariant.Accent,
                isLoading = true,
                full = true,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
