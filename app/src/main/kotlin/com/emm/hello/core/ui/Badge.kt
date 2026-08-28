package com.emm.hello.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.instrumentAccent
import com.emm.hello.core.theme.instrumentAccentSoft
import com.emm.hello.core.theme.instrumentGood
import com.emm.hello.core.theme.instrumentGoodSoft
import com.emm.hello.core.theme.instrumentMuted
import com.emm.hello.core.theme.instrumentWarn
import com.emm.hello.core.theme.instrumentWarnSoft
import com.emm.hello.core.theme.geistMono
import com.emm.hello.core.theme.semanticColors

enum class BadgeVariant { Default, Secondary, Destructive, Outline, Warning, Success, Tertiary }

enum class HBadgeTone { Accent, Good, Warn, Muted }

private val badgeShape = RoundedCornerShape(11.dp)
private val mutedBg = Color(red = 244, green = 239, blue = 230, alpha = 15)

@Composable
fun HBadge(
    label: String,
    modifier: Modifier = Modifier,
    tone: HBadgeTone = HBadgeTone.Accent,
) {
    val (bg, fg) = when (tone) {
        HBadgeTone.Accent -> instrumentAccentSoft to instrumentAccent
        HBadgeTone.Good -> instrumentGoodSoft to instrumentGood
        HBadgeTone.Warn -> instrumentWarnSoft to instrumentWarn
        HBadgeTone.Muted -> mutedBg to instrumentMuted
    }

    Surface(
        modifier = modifier,
        shape = badgeShape,
        color = bg,
        contentColor = fg,
    ) {
        Text(
            text = label,
            fontFamily = geistMono,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            letterSpacing = 0.2.sp,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun HBadge(
    label: String,
    modifier: Modifier = Modifier,
    variant: BadgeVariant = BadgeVariant.Default,
) {
    val tone = variant.toInstrumentTone()
    HBadge(label = label, modifier = modifier, tone = tone)
}

private fun BadgeVariant.toInstrumentTone(): HBadgeTone = when (this) {
    BadgeVariant.Default -> HBadgeTone.Accent
    BadgeVariant.Secondary -> HBadgeTone.Muted
    BadgeVariant.Destructive -> HBadgeTone.Muted
    BadgeVariant.Outline -> HBadgeTone.Muted
    BadgeVariant.Warning -> HBadgeTone.Warn
    BadgeVariant.Success -> HBadgeTone.Good
    BadgeVariant.Tertiary -> HBadgeTone.Good
}

@Composable
internal fun badgeColors(variant: BadgeVariant): Pair<Color, Color> {
    val cs = MaterialTheme.colorScheme
    return when (variant) {
        BadgeVariant.Default -> cs.primary to cs.onPrimary
        BadgeVariant.Secondary -> cs.surfaceContainerHighest to cs.onSurface
        BadgeVariant.Destructive -> cs.errorContainer to cs.onErrorContainer
        BadgeVariant.Outline -> Color.Transparent to cs.onSurface
        BadgeVariant.Warning ->
            MaterialTheme.semanticColors.warning.container to MaterialTheme.semanticColors.warning.content
        BadgeVariant.Success -> cs.tertiaryContainer to cs.onTertiaryContainer
        BadgeVariant.Tertiary -> cs.tertiaryContainer to cs.onTertiaryContainer
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun HBadgeInstrumentTonesPreview() {
    HelloTheme {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HBadge(label = "acento", tone = HBadgeTone.Accent)
            HBadge(label = "bien", tone = HBadgeTone.Good)
            HBadge(label = "aviso", tone = HBadgeTone.Warn)
            HBadge(label = "inactivo", tone = HBadgeTone.Muted)
        }
    }
}
