package com.emm.hello.core.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.instrumentPrimary

/**
 * Ember icon button — the single wrapper over Material3 [IconButton] so feature
 * screens never reference raw Material3. Preserves the M3 48dp touch target and
 * 24dp glyph by default; pass [buttonSize] for compact bars and [iconSize] for a
 * smaller glyph. [tint] defaults to instrumentPrimary.
 */
@Composable
fun HIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = instrumentPrimary,
    iconSize: Dp = 24.dp,
    buttonSize: Dp = 48.dp,
    enabled: Boolean = true,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(buttonSize),
        enabled = enabled,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            // Match Material3's disabled content alpha so a disabled button reads as
            // disabled — we override the default LocalContentColor tint, so the dim has
            // to be reapplied here or it would otherwise look enabled.
            tint = if (enabled) tint else tint.copy(alpha = DISABLED_ALPHA),
            modifier = Modifier.size(iconSize),
        )
    }
}

private const val DISABLED_ALPHA = 0.38f

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun HIconButtonPreview() {
    HelloTheme {
        HIconButton(
            icon = Icons.Default.MoreVert,
            contentDescription = null,
            onClick = {},
        )
    }
}
