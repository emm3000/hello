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
import com.emm.hello.core.theme.instrumentElev

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    containerColor: Color = instrumentElev,
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
