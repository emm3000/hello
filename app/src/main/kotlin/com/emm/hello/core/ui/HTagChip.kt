package com.emm.hello.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.emberAccent
import com.emm.hello.core.theme.emberAccentSoft
import com.emm.hello.core.theme.emberBg
import com.emm.hello.core.theme.emberElev
import com.emm.hello.core.theme.emberMuted
import com.emm.hello.core.theme.emberPrimary
import com.emm.hello.core.theme.geist

private val chipShape = RoundedCornerShape(15.dp)

/**
 * Ember-styled chip. Visual height is 30dp but tappable area is inflated to
 * ≥48dp via vertical padding for a11y.
 *
 * Props:
 * - [active]   — filled primary bg, dark fg
 * - [accent]   — accent-soft bg, accent fg
 * - [onRemove] — if set, shows close icon at right
 */
@Composable
fun HChip(
    label: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    accent: Boolean = false,
    onRemove: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val bg = when {
        active -> emberPrimary
        accent -> emberAccentSoft
        else -> Color.Transparent
    }
    val fg = when {
        active -> emberBg
        accent -> emberAccent
        else -> emberMuted
    }
    val hasBorder = !active && !accent

    // Outer box provides the a11y 48dp tap target
    Box(
        modifier = modifier.heightIn(min = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        val chipMod = Modifier
            .clip(chipShape)
            .then(if (hasBorder) Modifier.border(1.dp, emberElev, chipShape) else Modifier)
            .background(bg)
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                },
            )
            .padding(
                start = if (onRemove != null) 12.dp else 12.dp,
                end = if (onRemove != null) 8.dp else 12.dp,
                top = 6.dp,
                bottom = 6.dp,
            )

        Row(
            modifier = chipMod,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = label,
                fontFamily = geist,
                fontWeight = FontWeight.Medium,
                fontSize = 12.5.sp,
                letterSpacing = 0.1.sp,
                color = fg,
            )
            if (onRemove != null) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Quitar $label",
                    tint = fg.copy(alpha = 0.7f),
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}

// ── Legacy HTagChip — kept for backwards compat, delegates to HChip ────────

@Composable
fun HTagChip(
    tag: String,
    modifier: Modifier = Modifier,
    variant: BadgeVariant = BadgeVariant.Secondary,
    removable: Boolean = false,
    onRemove: () -> Unit = {},
) {
    val isAccent = variant == BadgeVariant.Default
    val isActive = variant == BadgeVariant.Success || variant == BadgeVariant.Tertiary
    HChip(
        label = tag,
        modifier = modifier,
        active = isActive,
        accent = isAccent,
        onRemove = if (removable) onRemove else null,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun HChipPreview() {
    HelloTheme {
        Surface(color = emberBg) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HChip(label = "inglés")
                HChip(label = "activo", active = true)
                HChip(label = "acento", accent = true)
                HChip(label = "quitar", onRemove = {})
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun HTagChipLegacyPreview() {
    HelloTheme {
        Surface(color = emberBg) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HTagChip(tag = "español")
                HTagChip(tag = "trabajo", variant = BadgeVariant.Outline)
                HTagChip(tag = "HSK3", variant = BadgeVariant.Default, removable = true, onRemove = {})
            }
        }
    }
}
