package com.emm.hello.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.instrumentAccent
import com.emm.hello.core.theme.instrumentBg
import com.emm.hello.core.theme.instrumentDivider
import com.emm.hello.core.theme.instrumentSurface

// ── Legacy enum kept for backwards compatibility ───────────────────────────
enum class CardVariant { Elevated, Filled, Outlined }

/**
 * Ember-styled card. Surface bg, 16dp radius, hairline divider border.
 * Optional [due] flag adds a 3dp accent left border for the "due" state.
 */
@Composable
fun HCard(
    modifier: Modifier = Modifier,
    due: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = modifier) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = instrumentSurface,
            border = BorderStroke(1.dp, instrumentDivider),
        ) {
            Column(content = content)
        }
        if (due) {
            // Accent left border — sits inside the card's left edge
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(
                        color = instrumentAccent,
                        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                    ),
            )
        }
    }
}

/**
 * Legacy overload — maps old [CardVariant] onto the Ember HCard so existing
 * call sites (feature screens) keep compiling unchanged.
 */
@Composable
fun HCard(
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") variant: CardVariant = CardVariant.Elevated,
    @Suppress("UNUSED_PARAMETER") cornerRadius: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    HCard(modifier = modifier, due = false, content = content)
}

/**
 * Elevated HCard overload for cases that require a real drop shadow, a custom
 * shape, or a custom container color (e.g. animated flip cards). Delegates to
 * M3 [Card] so [elevation] produces an actual shadow rather than a tonal shift.
 *
 * Use this variant only when the standard [HCard] cannot express the needed
 * appearance (different corner radius, Material elevation, or dynamic border).
 */
@Composable
fun HCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    elevation: Dp = 0.dp,
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        border = border,
        content = content,
    )
}

/** Header with optional title and subtitle. */
@Composable
fun HCardHeader(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** Main content area of the card. */
@Composable
fun HCardContent(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 0.dp),
        content = content,
    )
}

/** Footer area of the card. */
@Composable
fun HCardFooter(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        content = content,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun HCardPreview() {
    HelloTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(instrumentBg)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HCard(modifier = Modifier.fillMaxWidth()) {
                HCardHeader(title = "Serendipity", description = "/ˌserənˈdɪpɪti/ · Sustantivo")
                HCardContent {
                    Text(
                        text = "La ocurrencia de eventos afortunados de manera casual.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
            }
            HCard(modifier = Modifier.fillMaxWidth(), due = true) {
                HCardHeader(title = "Ephemeral", description = "5 tarjetas para hoy")
                HCardContent {
                    Text(
                        text = "Que dura poco tiempo.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
            }
        }
    }
}
