package com.emm.hello.newfeatures.card

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.emm.domain.flashcard.StaticCategories
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun BottomSheetDialogForPickCategory(
    onDismissRequest: (Boolean) -> Unit,
    showBottomSheet: Boolean,
    accounts: List<StaticCategories>,
    onAction: (StaticCategories) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dismiss: () -> Unit = { onDismissRequest(false) }

    if (showBottomSheet) {
        ModalBottomSheet(
            modifier = Modifier.statusBarsPadding(),
            onDismissRequest = dismiss,
            sheetState = sheetState,
        ) {
            val view = LocalView.current
            (view.parent as? DialogWindowProvider)?.window?.let { window ->
                SideEffect {
                    val insetsController = WindowCompat.getInsetsController(window, view)
                    insetsController.isAppearanceLightStatusBars = false
                    insetsController.isAppearanceLightNavigationBars = false
                }
            }
            AccountSelectorContent(
                categories = accounts,
                onAccountSelected = { onAction(it) },
                dismiss = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) dismiss()
                    }
                },
            )
        }
    }
}

@Composable
fun AccountSelectorContent(
    modifier: Modifier = Modifier,
    categories: List<StaticCategories>,
    onAccountSelected: (StaticCategories) -> Unit,
    dismiss: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = dismiss) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Cerrar",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                modifier = Modifier.align(Alignment.Center),
                text = "Categorías",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Spacer(Modifier.height(20.dp))

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            categories.forEach { category ->
                key(category.id) {
                    CategoryChip(category) { selected ->
                        onAccountSelected(selected)
                        dismiss()
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun CategoryChip(
    account: StaticCategories,
    onCardClick: (StaticCategories) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(100.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .clickable { onCardClick(account) },
    ) {
        Text(
            text = account.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}