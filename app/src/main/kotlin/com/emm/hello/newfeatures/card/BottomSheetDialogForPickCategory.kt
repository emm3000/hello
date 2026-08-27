package com.emm.hello.newfeatures.card

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import com.emm.hello.core.ui.HBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.emm.domain.catalog.StaticCategories
import com.emm.hello.R
import com.emm.hello.core.theme.instrumentAccent
import com.emm.hello.core.theme.instrumentDivider
import com.emm.hello.core.theme.instrumentElev
import com.emm.hello.core.theme.instrumentMuted
import com.emm.hello.core.theme.instrumentOnBg
import com.emm.hello.core.theme.geistMono
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetDialogForPickCategory(
    onDismissRequest: (Boolean) -> Unit,
    showBottomSheet: Boolean,
    accounts: List<StaticCategories>,
    selectedCategory: StaticCategories? = null,
    onAction: (StaticCategories) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dismiss: () -> Unit = { onDismissRequest(false) }

    if (showBottomSheet) {
        HBottomSheet(
            modifier = Modifier.statusBarsPadding(),
            onDismissRequest = dismiss,
            sheetState = sheetState,
            containerColor = instrumentElev,
            dragHandle = null,
        ) {
            val view = LocalView.current
            (view.parent as? DialogWindowProvider)?.window?.let { window ->
                SideEffect {
                    val insetsController = WindowCompat.getInsetsController(window, view)
                    insetsController.isAppearanceLightStatusBars = false
                    insetsController.isAppearanceLightNavigationBars = false
                }
            }
            CategoryListContent(
                categories = accounts,
                selectedCategory = selectedCategory,
                onCategorySelected = { category ->
                    onAction(category)
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) dismiss()
                    }
                },
            )
        }
    }
}

@Composable
private fun CategoryListContent(
    categories: List<StaticCategories>,
    selectedCategory: StaticCategories?,
    onCategorySelected: (StaticCategories) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Text(
            text = stringResource(R.string.categories_bottom_sheet_title).uppercase(),
            fontFamily = geistMono,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            letterSpacing = 0.12.em,
            color = instrumentMuted,
        )
        Spacer(Modifier.height(16.dp))
        LazyColumn(
            modifier = Modifier.heightIn(max = 480.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            items(items = categories, key = { it.id }) { category ->
                val isSelected = category == selectedCategory
                CategoryRow(
                    category = category,
                    isSelected = isSelected,
                    onClick = { onCategorySelected(category) },
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(instrumentDivider),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun CategoryRow(
    category: StaticCategories,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = category.name,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Normal,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            color = instrumentOnBg,
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = instrumentAccent,
                modifier = Modifier.size(22.dp),
            )
        } else {
            Spacer(Modifier.size(22.dp))
        }
    }
}
