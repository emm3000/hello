package com.emm.hello.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
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
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                    )
                },
            )
        }
    }
}

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
