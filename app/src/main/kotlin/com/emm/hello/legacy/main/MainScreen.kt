package com.emm.hello.legacy.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.emm.data.deprecated.word.WordEntity
import com.emm.hello.core.theme.HelloTheme
import java.util.UUID

@Composable
fun MainScreen(
    words: List<WordEntity>,
    wordSearch: String,
    onWordSearchUpdate: (String) -> Unit,
    wordCreate: (String) -> Unit,
    navigateToDetail: (String) -> Unit,
    navigateToBackup: () -> Unit,
    modifier: Modifier = Modifier,
) {

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val showDialog = remember {
        mutableStateOf(false)
    }

    if (showDialog.value) {
        AddWordBottomSheet(showDialog) {
            wordCreate(it)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            val switchVisualization = remember {
                mutableStateOf(true)
            }

            MainToolbar(
                navigateToBackup = navigateToBackup,
                wordSearch = wordSearch,
                switchVisualization = { switchVisualization.value = switchVisualization.value.not() },
                onWordSearchUpdate = onWordSearchUpdate,
                keyboardController = keyboardController,
                focusManager = focusManager
            )
            if (switchVisualization.value) {
                WordsInColumn(words, keyboardController, focusManager, navigateToDetail)
            } else {
                WordsInFlowColumn(words, keyboardController, focusManager, navigateToDetail)
            }
        }
        FloatingActionButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(25.dp),
            onClick = {
                keyboardController?.show()
                focusManager.clearFocus()
                showDialog.value = true
            },
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                focusedElevation = 0.dp,
                hoveredElevation = 0.dp
            )
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun MainScreenPreview() {
    HelloTheme {
        val items = remember {
            (1..50).map {
                WordEntity(
                    id = it.toString(),
                    word = UUID.randomUUID().toString().take(4),
                    hasContent = true,
                    createdAt = 0L
                )
            }
        }
        MainScreen(
            words = items,
            onWordSearchUpdate = {},
            wordSearch = "",
            wordCreate = {},
            navigateToDetail = {},
            navigateToBackup = {},
            modifier = Modifier,
        )
    }
}