package com.emm.hello.core.ui

import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.emberElev

/**
 * Ember-styled modal bottom sheet. Defaults to [emberElev] container, no drag
 * handle — matching the app's existing bottom sheet look.
 *
 * @param onDismissRequest called when the sheet should be dismissed.
 * @param sheetState pass a [rememberModalBottomSheetState] instance when you
 *   need programmatic show/hide (e.g. animated hide before dismissal) or want
 *   to skip the partially-expanded state.
 * @param containerColor background of the sheet surface.
 * @param dragHandle optional composable for a drag handle; null hides it.
 * @param modifier applied to the bottom sheet root.
 * @param content sheet content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    containerColor: Color = emberElev,
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    dragHandle: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        containerColor = containerColor,
        shape = shape,
        dragHandle = dragHandle,
        content = { content() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun HBottomSheetPreview() {
    HelloTheme {
        HBottomSheet(onDismissRequest = {}) {
            Text("Sheet content")
        }
    }
}
