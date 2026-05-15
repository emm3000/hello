package com.emm.hello.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.emm.hello.core.theme.HelloTheme

/**
 * Primary tab bar inspired by shadcn/ui `<Tabs />`.
 *
 * Usage: top-level navigation inside a screen when content splits into
 * a small, fixed set of buckets (3-5 max). For larger sets, use sections.
 */
@Composable
fun HTabBar(
    selectedIndex: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    badges: List<TabBadge?> = emptyList(),
) {
    PrimaryTabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
    ) {
        tabs.forEachIndexed { index, label ->
            Tab(
                selected = index == selectedIndex,
                onClick = { onTabSelected(index) },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                        )
                        badges.getOrNull(index)?.let { badge ->
                            HBadge(
                                label = badge.label,
                                variant = badge.variant,
                            )
                        }
                    }
                },
            )
        }
    }
}

data class TabBadge(
    val label: String,
    val variant: BadgeVariant = BadgeVariant.Destructive,
)

@PreviewLightDark
@Composable
private fun HTabBarPreview() {
    HelloTheme {
        HTabBar(
            selectedIndex = 0,
            tabs = listOf("Resumen", "Ejemplos", "Estudio"),
            onTabSelected = {},
        )
    }
}
