package com.emm.hello.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.helloShapes
import com.emm.hello.core.theme.ink
import com.emm.hello.core.theme.inkMuted
import com.emm.hello.core.theme.spacing
import com.emm.hello.core.theme.successContainer
import com.emm.hello.core.theme.successInk
import com.emm.hello.core.theme.surfaceRaised
import com.emm.hello.core.theme.warningContainer
import com.emm.hello.core.theme.warningInk

enum class BadgeVariant { Default, Secondary, Destructive, Outline, Warning, Success, Tertiary }

enum class HBadgeTone { Accent, Good, Warn, Muted }

@Composable
fun HBadge(
    label: String,
    modifier: Modifier = Modifier,
    tone: HBadgeTone = HBadgeTone.Accent,
) {
    val (bg: Color, fg: Color) = when (tone) {
        HBadgeTone.Accent -> surfaceRaised to ink
        HBadgeTone.Good -> successContainer to successInk
        HBadgeTone.Warn -> warningContainer to warningInk
        HBadgeTone.Muted -> surfaceRaised to inkMuted
    }
    val badgeModifier: Modifier = if (tone == HBadgeTone.Warn) {
        modifier.semantics { stateDescription = "Advertencia" }
    } else {
        modifier
    }

    Surface(
        modifier = badgeModifier,
        shape = MaterialTheme.helloShapes.pill,
        color = bg,
        contentColor = fg,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(
                horizontal = MaterialTheme.spacing.md,
                vertical = MaterialTheme.spacing.xs,
            ),
        )
    }
}

@Composable
fun HBadge(
    label: String,
    modifier: Modifier = Modifier,
    variant: BadgeVariant = BadgeVariant.Default,
) {
    val tone: HBadgeTone = variant.toTone()
    HBadge(label = label, modifier = modifier, tone = tone)
}

private fun BadgeVariant.toTone(): HBadgeTone = when (this) {
    BadgeVariant.Default -> HBadgeTone.Accent
    BadgeVariant.Secondary -> HBadgeTone.Muted
    BadgeVariant.Destructive -> HBadgeTone.Muted
    BadgeVariant.Outline -> HBadgeTone.Muted
    BadgeVariant.Warning -> HBadgeTone.Warn
    BadgeVariant.Success -> HBadgeTone.Good
    BadgeVariant.Tertiary -> HBadgeTone.Good
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun HBadgeTonesPreview() {
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
