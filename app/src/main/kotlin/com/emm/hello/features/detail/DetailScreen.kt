package com.emm.hello.features.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.domain.deprecated.word.Example
import com.emm.domain.deprecated.word.SourceType
import com.emm.domain.deprecated.word.Word
import com.emm.domain.deprecated.word.WordContent
import com.emm.hello.core.theme.HelloTheme
import kotlinx.coroutines.launch

@Composable
fun DetailScreen(
    state: DetailUiState,
    wordName: String,
    updateWord: (String) -> Unit,
    deleteWord: () -> Unit,
    generateContent: (SourceType) -> Unit,
    modifier: Modifier = Modifier,
) {

    val (showDialog, setShowDialog) = remember {
        mutableStateOf(false)
    }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { setShowDialog(false) },
            confirmButton = {
                TextButton(
                    onClick = {
                        setShowDialog(false)
                        deleteWord()
                    }
                ) {
                    Text(text = "Aceptar", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        setShowDialog(false)
                    }
                ) {
                    Text(text = "Cancelar", color = MaterialTheme.colorScheme.tertiary)
                }
            },
            title = { Text(text = "Estas seguro de eliminar") },
            text = { Text(text = "Estas seguro de eliminar") },
            icon = { Icon(Icons.Outlined.Delete, contentDescription = "Example Icon") },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        val (isEditable, setIsEditable) = remember {
            mutableStateOf(false)
        }

        val (wordField, setWordField) = remember {
            mutableStateOf(TextFieldValue(wordName))
        }

        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(isEditable) {
            if (isEditable) {
                focusRequester.requestFocus()
                setWordField(
                    wordField.copy(selection = TextRange(wordField.text.length))
                )
            }
        }

        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp)
                .focusRequester(focusRequester),
            value = wordField,
            onValueChange = setWordField,
            enabled = isEditable,
            textStyle = TextStyle(
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            ),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onBackground,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                disabledBorderColor = Color.Transparent,
                errorBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,

                ),
            trailingIcon = {
                if (isEditable) {
                    EditableIconsButtons(setWordField, updateWord, wordField, setIsEditable)
                } else {
                    NonEditableIconButton(setIsEditable, onDelete = { setShowDialog(true) })
                }
            },
        )

        val titles: List<String> = listOf("Dictionary", "Gemini", "Anki")
        val pagerState: PagerState = rememberPagerState { titles.size }
        val rememberCoroutineScope = rememberCoroutineScope()
        val currentIndex by remember { derivedStateOf { pagerState.currentPage } }

        TabRow(
            selectedTabIndex = currentIndex,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .clip(RoundedCornerShape(25)),
            indicator = {
                if (currentIndex < it.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(it[currentIndex]),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        ) {
            titles.forEachIndexed { index, s ->
                Tab(
                    modifier = Modifier.height(40.dp),
                    selected = currentIndex == index,
                    onClick = {
                        rememberCoroutineScope.launch { pagerState.animateScrollToPage(index) }
                    }
                ) {
                    Text(
                        text = s,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        HorizontalPager(
            modifier = Modifier,
            state = pagerState,
        ) {
            when (it) {
                0 -> DictionaryScreen(
                    state = state,
                    generateContent = generateContent,
                )

                1 -> IaScreen(
                    state = state,
                    generateContent = generateContent
                )

                2 -> AnkiScreen(
                    state = state,
                    generateContent = generateContent
                )
            }

        }
    }
}


@Composable
private fun EditableIconsButtons(
    setWordField: (TextFieldValue) -> Unit,
    updateWord: (String) -> Unit,
    wordField: TextFieldValue,
    setIsEditable: (Boolean) -> Unit
) {
    Row {
        IconButton(onClick = { setWordField(TextFieldValue()) }) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        IconButton(
            onClick = {
                updateWord(wordField.text)
                setIsEditable(false)
            }
        ) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun NonEditableIconButton(
    setIsEditable: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    Row {
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        IconButton(onClick = {
            setIsEditable(true)
        }) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun DetailScreenPreview() {
    HelloTheme {
        DetailScreen(
            state = DetailUiState(
                currentWord = Word(
                    id = "non",
                    word = "consectetur",
                    hasContent = true,
                    createdAt = 0L
                ),
                scrapContentWord = WordContent(
                    wordContentId = "",
                    word = "gaa",
                    pos = "gaaa x2",
                    sourceType = SourceType.SCRAPPING,
                    examples = listOf(
                        Example(
                            number = "1",
                            title = "random title",
                            sentences = listOf(
                                "random title d ASLCK AS KNCASL CNals kcas lm",
                                "random title 3"
                            )
                        ), Example(
                            number = "1",
                            title = "random title",
                            sentences = listOf()
                        )
                    )
                )
            ),
            wordName = "random word",
            generateContent = {},
            deleteWord = {},
            updateWord = { }
        )
    }
}