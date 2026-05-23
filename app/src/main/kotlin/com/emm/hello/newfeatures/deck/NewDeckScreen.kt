package com.emm.hello.newfeatures.deck

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.emm.hello.R
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.emberAccent
import com.emm.hello.core.theme.emberBg
import com.emm.hello.core.theme.emberDivider
import com.emm.hello.core.theme.emberMuted
import com.emm.hello.core.theme.emberOnBg
import com.emm.hello.core.theme.emberPrimary
import com.emm.hello.core.theme.emberSurface
import com.emm.hello.core.theme.geist
import com.emm.hello.core.theme.geistMono
import com.emm.hello.core.theme.instrumentSerif
import com.emm.hello.core.ui.HChip
import com.emm.hello.core.ui.HInput

@Composable
fun NewDeckScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    state: NewDeckUiState = NewDeckUiState(),
    onIntent: (NewDeckUiIntent) -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    val isEditMode = state.formMode is DeckFormMode.Edit
    val topLabel = if (isEditMode) {
        stringResource(R.string.edit_deck_top_label)
    } else {
        stringResource(R.string.new_deck_top_label)
    }
    val headline = if (isEditMode) {
        stringResource(R.string.edit_deck_headline)
    } else {
        stringResource(R.string.new_deck_headline)
    }
    val actionLabel = if (isEditMode) {
        stringResource(R.string.new_deck_action_save)
    } else {
        stringResource(R.string.new_deck_action_create)
    }
    val nameFocusRequester = remember { FocusRequester() }

    LaunchedEffect(state.formMode) {
        if (!isEditMode) {
            nameFocusRequester.requestFocus()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = emberBg,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .imePadding(),
        ) {
            NewDeckTopBar(
                title = topLabel,
                actionLabel = actionLabel,
                actionEnabled = state.isValid && !state.isLoading,
                onClose = onNavigateBack,
                onAction = {
                    focusManager.clearFocus()
                    onIntent(NewDeckUiIntent.Submit)
                },
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 6.dp, bottom = 32.dp),
            ) {
                Text(
                    text = headline,
                    fontFamily = instrumentSerif,
                    fontSize = 44.sp,
                    lineHeight = (44 * 1.04f).sp,
                    letterSpacing = (-0.5).sp,
                    color = emberPrimary,
                )

                Spacer(Modifier.height(28.dp))

                DeckNameField(
                    value = state.name,
                    onValueChange = { onIntent(NewDeckUiIntent.NameChanged(it)) },
                    focusRequester = nameFocusRequester,
                    placeholder = stringResource(R.string.deck_name_placeholder),
                )

                Spacer(Modifier.height(20.dp))

                HInput(
                    value = state.description,
                    onValueChange = { onIntent(NewDeckUiIntent.DescriptionChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(R.string.deck_description_label),
                    placeholder = stringResource(R.string.deck_description_placeholder),
                    supportingText = stringResource(R.string.optional),
                    singleLine = false,
                    minLines = 3,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() },
                    ),
                )

                Spacer(Modifier.height(20.dp))

                EmberTagsField(
                    tags = state.tags,
                    onTagsChange = { onIntent(NewDeckUiIntent.TagsChanged(it)) },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top bar — close · mono title · accent text action
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NewDeckTopBar(
    title: String,
    actionLabel: String,
    actionEnabled: Boolean,
    onClose: () -> Unit,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(44.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.new_deck_close_content_description),
                tint = emberPrimary,
            )
        }

        Text(
            text = title.uppercase(),
            fontFamily = geistMono,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            letterSpacing = 0.12.em,
            color = emberMuted,
            modifier = Modifier.weight(1f),
        )

        val actionColor = if (actionEnabled) emberAccent else emberMuted
        Text(
            text = actionLabel,
            fontFamily = geist,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            color = actionColor,
            modifier = Modifier
                .padding(end = 6.dp)
                .then(if (actionEnabled) Modifier.clickable(onClick = onAction) else Modifier)
                .padding(horizontal = 10.dp, vertical = 10.dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Deck name — 22sp italic serif input with accent border
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DeckNameField(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    placeholder: String,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.deck_name_label).uppercase(),
            fontFamily = geistMono,
            fontWeight = FontWeight.Medium,
            fontSize = 10.5.sp,
            letterSpacing = 0.12.em,
            color = emberMuted,
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(emberSurface, RoundedCornerShape(14.dp))
                .border(1.5.dp, emberAccent, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    fontFamily = instrumentSerif,
                    fontStyle = FontStyle.Italic,
                    fontSize = 22.sp,
                    color = emberMuted,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = instrumentSerif,
                    fontStyle = FontStyle.Italic,
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    color = emberOnBg,
                ),
                cursorBrush = SolidColor(emberAccent),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tags container — emberSurface with removable chips + inline "+ agregar…"
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmberTagsField(
    tags: List<String>,
    onTagsChange: (List<String>) -> Unit,
) {
    var inputValue by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val commitTag: () -> Unit = {
        val normalized = inputValue.trimEnd(',').trim().lowercase()
        if (normalized.isNotEmpty() &&
            tags.none { it.equals(normalized, ignoreCase = true) }
        ) {
            onTagsChange(tags + normalized)
        }
        inputValue = ""
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.tags_label).uppercase(),
            fontFamily = geistMono,
            fontWeight = FontWeight.Medium,
            fontSize = 10.5.sp,
            letterSpacing = 0.12.em,
            color = emberMuted,
        )
        Spacer(Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(emberSurface, RoundedCornerShape(14.dp))
                .border(1.dp, emberDivider, RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                tags.forEach { tag ->
                    HChip(
                        label = tag,
                        accent = true,
                        onRemove = { onTagsChange(tags - tag) },
                    )
                }
                TagInline(
                    value = inputValue,
                    onValueChange = { raw ->
                        if (raw.endsWith(",")) {
                            inputValue = raw.dropLast(1)
                            commitTag()
                        } else {
                            inputValue = raw
                        }
                    },
                    onDoneAction = {
                        commitTag()
                        focusManager.clearFocus()
                    },
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.tags_supporting_text),
            fontFamily = geist,
            fontSize = 12.sp,
            color = emberMuted,
        )
    }
}

@Composable
private fun TagInline(
    value: String,
    onValueChange: (String) -> Unit,
    onDoneAction: () -> Unit,
) {
    val placeholder = stringResource(R.string.new_deck_tag_placeholder)
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp, vertical = 8.dp)
            .width(140.dp),
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                fontFamily = geist,
                fontSize = 13.sp,
                color = emberMuted,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = geist,
                fontSize = 13.sp,
                color = emberOnBg,
            ),
            cursorBrush = SolidColor(emberAccent),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = { onDoneAction() },
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

@PreviewLightDark
@Composable
private fun NewDeckCreatePreview() {
    HelloTheme {
        NewDeckScreen(
            state = NewDeckUiState(
                name = "",
                description = "",
                tags = emptyList(),
                formMode = DeckFormMode.Create,
            ),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun NewDeckEditPreview() {
    HelloTheme {
        NewDeckScreen(
            state = NewDeckUiState(
                name = "Vocabulario editorial",
                description = "Palabras curadas para probar el detalle.",
                tags = listOf("inglés", "editorial"),
                formMode = DeckFormMode.Edit("deck-1"),
            ),
        )
    }
}
