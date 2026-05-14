package com.emm.hello.newfeatures.card

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emm.domain.deck.Deck
import com.emm.data.catalog.staticCategories
import com.emm.domain.ids.toDeckId
import com.emm.hello.R
import com.emm.hello.newfeatures.card.validation.IssueTextMapper
import com.emm.hello.core.audio.rememberSpeechToTextManager
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.metadata
import com.emm.hello.core.theme.spacing
import com.emm.hello.core.ui.AlertVariant
import com.emm.hello.core.ui.HAlert
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HSelect
import com.emm.hello.core.ui.HSectionBlock
import java.time.LocalDateTime
import java.util.Locale

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun NewCardScreen(
    modifier: Modifier = Modifier,
    state: NewCardUiState = NewCardUiState(),
    onIntent: (NewCardUiIntent) -> Unit = {},
    onNavigateBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val sttManager = rememberSpeechToTextManager { voiceText ->
        onIntent(NewCardUiIntent.WordChanged(voiceText))
    }
    val isListening by sttManager.isListening.collectAsStateWithLifecycle()
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) sttManager.startListening(Locale.US)
        },
    )
    val toggleVoiceInput = {
        if (isListening) {
            sttManager.stopListening()
        } else if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            sttManager.startListening(Locale.US)
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val showBottomSheet = remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val issueTextMapper = remember { IssueTextMapper() }
    val hasPreview = state.learningNotePreview != null
    val isGenerateEnabled = state.isGenerateEnabled()

    LaunchedEffect(hasPreview) {
        if (hasPreview) {
            listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.new_card_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            contentPadding = innerPadding,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MaterialTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg + MaterialTheme.spacing.xs),
        ) {
            item {
                HSectionBlock(
                    title = stringResource(R.string.creation_mode_section_title),
                    description = stringResource(R.string.creation_mode_section_description),
                ) {
                    InputModeSelector(
                        selectedMode = state.typeView,
                        onModeSelected = { onIntent(NewCardUiIntent.TypeViewSelected(it)) },
                    )
                }
            }

            item {
                HSectionBlock(
                    title = stringResource(R.string.input_section_title),
                    description = stringResource(state.typeView.inputSectionDescriptionResId()),
                ) {
                    NewCardInputSection(
                        state = state,
                        isListening = isListening,
                        onIntent = onIntent,
                        onToggleVoiceInput = toggleVoiceInput,
                        onShowCategoryPicker = { showBottomSheet.value = true },
                    )
                }
            }

            item {
                HSectionBlock(
                    title = stringResource(R.string.destination_section_title),
                    description = stringResource(R.string.destination_section_description),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)) {
                        HSelect(
                            items = state.decks,
                            itemSelected = state.deckSelected,
                            enabled = !state.isLoading,
                            onItemSelected = { onIntent(NewCardUiIntent.DeckSelected(it)) },
                            label = stringResource(R.string.deck_label),
                            placeholder = stringResource(R.string.select_deck_placeholder),
                            itemLabel = { it.name },
                        )

                        AnimatedVisibility(
                            visible = state.decks.isNotEmpty(),
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            LabeledCheckbox(
                                label = stringResource(R.string.default_deck_checkbox),
                                checked = state.isCheck,
                                isEnabled = state.deckSelected != null,
                                onCheckedChange = { onIntent(NewCardUiIntent.CheckChanged(it)) },
                            )
                        }
                    }
                }
            }

            if (!hasPreview) {
                item {
                    HButton(
                        text = stringResource(R.string.generate_card),
                        onClick = {
                            keyboardController?.hide()
                            onIntent(NewCardUiIntent.GenerateClicked)
                        },
                        enabled = isGenerateEnabled,
                        isLoading = state.isLoading,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (state.error != null) {
                item {
                    HAlert(
                        title = state.error.title,
                        description = state.error.localizedDescription(issueTextMapper),
                        variant = AlertVariant.Destructive,
                    )
                }
            }

            if (state.isLoading) {
                item { LoadingPreviewSkeleton() }
            }

            if (hasPreview) {
                item {
                    ResultPreviewSection(
                        state = state,
                        keyboardController = keyboardController,
                        onIntent = onIntent,
                    )
                }
                item { Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg + MaterialTheme.spacing.xs)) }
            }
        }
    }

    BottomSheetDialogForPickCategory(
        onDismissRequest = { showBottomSheet.value = it },
        showBottomSheet = showBottomSheet.value,
        accounts = staticCategories,
        selectedCategory = state.category,
        onAction = { onIntent(NewCardUiIntent.CategorySelected(it)) },
    )
}

@Composable
internal fun LabeledCheckbox(
    label: String,
    checked: Boolean,
    isEnabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { onCheckedChange(!checked) }, enabled = isEnabled)
            .padding(MaterialTheme.spacing.sm),
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
internal fun SupportingText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.metadata,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun NewCardUiState.isGenerateEnabled(): Boolean {
    val hasDeck = deckSelected != null
    val isReady = !isLoading
    return when (typeView) {
        TypeView.WordOrPhase -> isReady && hasDeck && word.isNotBlank()
        TypeView.WithCategories -> isReady && hasDeck
        TypeView.WithAiHelp -> isReady && hasDeck && aiRequest.isNotBlank()
    }
}

private fun TypeView.inputSectionDescriptionResId(): Int {
    return when (this) {
        TypeView.WordOrPhase -> R.string.input_section_word_description
        TypeView.WithCategories -> R.string.input_section_category_description
        TypeView.WithAiHelp -> R.string.input_section_ai_description
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun NewCardScreenPreview() {
    HelloTheme {
        NewCardScreen(
            state = NewCardUiState(
                word = "Serendipity",
                decks = listOf(
                    Deck(
                        id = "1".toDeckId(),
                        name = "Vocabulario Inglés",
                        description = "",
                        createdAt = LocalDateTime.now(),
                        cards = emptyList(),
                        cardsCount = 24,
                    ),
                ),
            ),
        )
    }
}
