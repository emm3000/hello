package com.emm.hello.features.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.data.word.WordEntity
import com.emm.hello.core.theme.HelloTheme
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
    words: List<WordEntity>,
    wordSearch: String,
    onWordSearchUpdate: (String) -> Unit,
    navigateToDetail: (String) -> Unit,
    navigateToBackup: () -> Unit,
    navigateToAddWord: () -> Unit,
    modifier: Modifier = Modifier,
) {

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

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
                LazyColumn(
                    modifier = Modifier
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(start = 5.dp, end = 5.dp, bottom = 100.dp),
                ) {
                    items(words, key = WordEntity::id) { wordEntity ->
                        WordItem(
                            keyboardController = keyboardController,
                            focusManager = focusManager,
                            navigateToDetail = navigateToDetail,
                            wordEntity = wordEntity,
                        )
                    }
                }
            } else {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 10.dp, bottom = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    words.forEach { wordEntity ->
                        key(wordEntity.id) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20))
                                    .border(
                                        width = 1.dp,
                                        color = if (wordEntity.hasContent) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                        } else MaterialTheme.colorScheme.inverseOnSurface,
                                        shape = RoundedCornerShape(20)
                                    )
                                    .background(MaterialTheme.colorScheme.surfaceContainer)
                                    .clickable {
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                        navigateToDetail(wordEntity.id)
                                    }
                                    .padding(vertical = 5.dp, horizontal = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    modifier = Modifier,
                                    text = wordEntity.word,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 17.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    color = if (wordEntity.hasContent) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                    } else LocalContentColor.current,
                                    textAlign = TextAlign.Start,
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(25.dp),
            onClick = {
                keyboardController?.hide()
                focusManager.clearFocus()
                navigateToAddWord()
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

@Composable
private fun ColumnScope.MainToolbar(
    navigateToBackup: () -> Unit,
    switchVisualization: () -> Unit,
    wordSearch: String,
    onWordSearchUpdate: (String) -> Unit,
    keyboardController: SoftwareKeyboardController?,
    focusManager: FocusManager
) {
    val showSearchBar = remember {
        mutableStateOf(false)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = "Learning",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 30.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
        )
        IconButton(onClick = switchVisualization) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        IconButton(onClick = navigateToBackup) {
            Icon(
                imageVector = Icons.Rounded.Build,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }

        IconButton(onClick = {
            showSearchBar.value = !showSearchBar.value
        }) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
    }

    AnimatedVisibility(showSearchBar.value) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = wordSearch,
            singleLine = true,
            onValueChange = {
                onWordSearchUpdate(it)
            },
            label = {
                Text("Search Word")
            },
            trailingIcon = {
                LeadingIcon {
                }
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Unspecified,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search,
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }
            )
        )
    }
}

@Composable
private fun WordItem(
    keyboardController: SoftwareKeyboardController?,
    focusManager: FocusManager,
    navigateToDetail: (String) -> Unit,
    wordEntity: WordEntity
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable {
                keyboardController?.hide()
                focusManager.clearFocus()
                navigateToDetail(wordEntity.id)
            }
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier
                .weight(1f),
            text = wordEntity.word,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            fontFamily = FontFamily.SansSerif,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Start,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
        if (wordEntity.hasContent) {
            Text(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .clip(RoundedCornerShape(20))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(20)
                    )
                    .padding(horizontal = 5.dp),
                text = "content",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            )
        }
        Icon(
            modifier = Modifier.padding(start = 10.dp),
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun LeadingIcon(onSave: () -> Unit) {
    IconButton(onClick = { onSave() }) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
        )
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
            navigateToDetail = {},
            navigateToBackup = {},
            navigateToAddWord = {},
            modifier = Modifier,
        )
    }
}