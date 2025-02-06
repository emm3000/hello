package com.emm.hello.features.detail

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.hello.core.theme.HelloTheme

@Composable
fun DetailScreen(
    state: DetailUiState,
    wordName: String,
    updateWord: (String) -> Unit,
    generateContent: () -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
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
                .focusRequester(focusRequester),
            value = wordField,
            onValueChange = setWordField,
            enabled = isEditable,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onBackground,
                disabledBorderColor = MaterialTheme.colorScheme.onBackground
            ),
            trailingIcon = {
                if (isEditable) {
                    EditableIconsButtons(setWordField, updateWord, wordField, setIsEditable)
                } else {
                    NonEditableIconButton(setIsEditable)
                }
            },
        )

        if (state.hasContent.not()) {

            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .padding(bottom = 15.dp),
                    text = "Content not found, can you generate content?",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.ExtraBold
                )

                Box(
                    modifier = Modifier
                        .padding(bottom = 150.dp)
                        .height(70.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.isLoading) {
                        LettersLoading()
                    } else {
                        OutlinedButton(
                            onClick = generateContent,
                            shape = RoundedCornerShape(20)
                        ) {
                            Text(
                                text = "Generate content",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                            )
                        }
                    }
                }
            }
        }

        if (state.contentWord != null) {

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Text(
                            text = state.contentWord.word,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = state.contentWord.pos,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                items(state.contentWord.examples) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(2),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.8f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(13.dp)
                        ) {
                            Text(
                                text = "${it.number}. ${it.title}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(10.dp))
                            it.sentences.forEach {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = "✅",
                                        fontSize = 15.sp,
                                    )
                                    Text(
                                        text = it,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Light,
                                        color = LocalContentColor.current.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LettersLoading() {
    val letters = "Loading...".toList()
    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        letters.forEachIndexed { index, letter ->
            val animatedOffset by rememberInfiniteTransition().animateFloat(
                initialValue = 0f,
                targetValue = -8f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 100)
                )
            )

            Text(
                text = letter.toString(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .offset(y = animatedOffset.dp)
                    .padding(2.dp),
                color = MaterialTheme.colorScheme.primary
            )
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
) {
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

@PreviewLightDark
@Composable
private fun DetailScreenPreview() {
    HelloTheme {
        DetailScreen(
            state = DetailUiState(
//                contentWord = WordContent(
//                    wordId = "",
//                    word = "gaa",
//                    pos = "gaaa x2",
//                    examples = listOf(
//                        Example(
//                            number = "1",
//                            title = "random title",
//                            sentences = listOf(
//                                "random title d ASLCK AS KNCASL CNals kcas lm",
//                                "random title 3"
//                            )
//                        )
//                    )
//                )
            ),
            wordName = "random word",
            generateContent = {},
            updateWord = { }
        )
    }
}