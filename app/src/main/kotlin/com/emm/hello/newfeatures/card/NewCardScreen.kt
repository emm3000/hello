package com.emm.hello.newfeatures.card

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emm.domain.deck.Deck
import com.emm.domain.flashcard.EvaluationMode
import com.emm.domain.flashcard.Example
import com.emm.domain.flashcard.GeneratedLearningNote
import com.emm.domain.flashcard.GeneratedLearningNoteIssue
import com.emm.domain.flashcard.GeneratedNoteQualityCheck
import com.emm.domain.flashcard.GeneratedNoteQualityCode
import com.emm.domain.flashcard.GeneratedStudyCard
import com.emm.domain.flashcard.LearningDomain
import com.emm.domain.flashcard.LearningNoteType
import com.emm.domain.flashcard.LevelBand
import com.emm.domain.flashcard.PartOfSpeechTag
import com.emm.domain.flashcard.RegisterPreference
import com.emm.domain.flashcard.StudyCardType
import com.emm.domain.flashcard.TypeView
import com.emm.domain.flashcard.difficult
import com.emm.domain.flashcard.staticCategories
import com.emm.hello.R
import com.emm.hello.core.audio.rememberSpeechToTextManager
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.ui.AlertVariant
import com.emm.hello.core.ui.BadgeVariant
import com.emm.hello.core.ui.ButtonVariant
import com.emm.hello.core.ui.CardVariant
import com.emm.hello.core.ui.HAlert
import com.emm.hello.core.ui.HBadge
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HCard
import com.emm.hello.core.ui.HInput
import com.emm.hello.core.ui.HSelect
import com.emm.hello.core.ui.HSeparator
import com.emm.hello.core.ui.HSkeleton
import java.time.LocalDateTime
import java.util.Locale

private const val SKELETON_DETAIL_WIDTH = 0.4f
private const val RESULT_FADE_IN_DURATION_MS = 300
private const val MAX_PREVIEW_COLLOCATIONS = 3

private data class PreviewAlertModel(
    val title: String,
    val description: String,
    val variant: AlertVariant,
)

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
        }
    )

    val toggleVoiceInput = {
        if (isListening) {
            sttManager.stopListening()
        } else {
            val hasPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                sttManager.startListening(Locale.US)
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    val showBottomSheet = remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val hasPreview = state.learningNotePreview != null

    val isGenerateEnabled = remember(state.isLoading, state.deckSelected, state.word, state.typeView) {
        val hasWord = state.word.isNotBlank()
        val hasDeck = state.deckSelected != null
        val notLoading = !state.isLoading
        when (state.typeView) {
            TypeView.WordOrPhase -> notLoading && hasDeck && hasWord
            TypeView.WithCategories -> notLoading && hasDeck
        }
    }

    LaunchedEffect(hasPreview) {
        if (hasPreview) {
            // Scroll to last item (result preview) after a brief delay for layout
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                SectionCard(
                    title = stringResource(R.string.creation_mode_section_title),
                    description = stringResource(R.string.creation_mode_section_description),
                ) {
                    InputModeSelector(
                        selectedMode = state.typeView,
                        onModeSelected = { onIntent(NewCardUiIntent.TypeViewSelected(it)) },
                    )
                }
            }

            // -- Input Section --------------------------------------------------------
            item {
                SectionCard(
                    title = stringResource(R.string.input_section_title),
                    description = when (state.typeView) {
                        TypeView.WordOrPhase -> stringResource(R.string.input_section_word_description)
                        TypeView.WithCategories -> stringResource(R.string.input_section_category_description)
                    },
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

            // -- Destination Section --------------------------------------------------
            item {
                SectionCard(
                    title = stringResource(R.string.destination_section_title),
                    description = stringResource(R.string.destination_section_description),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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

            // -- Generate Button ------------------------------------------------------
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    )
                }
            }

            // -- Error Display --------------------------------------------------------
            if (state.error != null) {
                item {
                    HAlert(
                        title = state.error.title,
                        description = state.error.message,
                        variant = AlertVariant.Destructive,
                    )
                }
            }

            // -- Loading Skeleton -----------------------------------------------------
            if (state.isLoading) {
                item {
                    LoadingPreviewSkeleton()
                }
            }

            // -- Result Preview -------------------------------------------------------
            if (hasPreview) {
                item {
                    ResultPreviewSection(
                        state = state,
                        keyboardController = keyboardController,
                        onIntent = onIntent,
                    )
                }
                item { Spacer(modifier = Modifier.height(20.dp)) }
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
private fun NewCardInputSection(
    state: NewCardUiState,
    isListening: Boolean,
    onIntent: (NewCardUiIntent) -> Unit,
    onToggleVoiceInput: () -> Unit,
    onShowCategoryPicker: () -> Unit,
) {
    when (state.typeView) {
        TypeView.WordOrPhase -> WordOrPhraseInputSection(
            state = state,
            isListening = isListening,
            onIntent = onIntent,
            onToggleVoiceInput = onToggleVoiceInput,
        )

        TypeView.WithCategories -> CategoryInputSection(
            state = state,
            onIntent = onIntent,
            onShowCategoryPicker = onShowCategoryPicker,
        )
    }
}

@Composable
private fun InputModeSelector(
    selectedMode: TypeView,
    onModeSelected: (TypeView) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ModeOptionCard(
            title = stringResource(R.string.new_card_mode_word),
            description = stringResource(R.string.mode_word_description),
            helperText = stringResource(R.string.mode_word_helper),
            isSelected = selectedMode == TypeView.WordOrPhase,
            onClick = { onModeSelected(TypeView.WordOrPhase) },
        )
        ModeOptionCard(
            title = stringResource(R.string.new_card_mode_category),
            description = stringResource(R.string.mode_category_description),
            helperText = stringResource(R.string.mode_category_helper),
            isSelected = selectedMode == TypeView.WithCategories,
            onClick = { onModeSelected(TypeView.WithCategories) },
        )
    }
}

@Composable
private fun ModeOptionCard(
    title: String,
    description: String,
    helperText: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    HCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        variant = if (isSelected) CardVariant.Filled else CardVariant.Outlined,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isSelected) {
                    HBadge(
                        label = stringResource(R.string.mode_selected_badge),
                        variant = BadgeVariant.Success,
                    )
                }
            }
            Text(
                text = helperText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WordOrPhraseInputSection(
    state: NewCardUiState,
    isListening: Boolean,
    onIntent: (NewCardUiIntent) -> Unit,
    onToggleVoiceInput: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HInput(
            value = state.word,
            onValueChange = { onIntent(NewCardUiIntent.WordChanged(it)) },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.word_label),
            placeholder = if (isListening) {
                stringResource(R.string.listening_placeholder)
            } else {
                stringResource(R.string.word_placeholder)
            },
            supportingText = stringResource(R.string.word_supporting_text),
            trailingIcon = {
                VoiceInputButton(
                    isListening = isListening,
                    onClick = onToggleVoiceInput
                )
            }
        )
        HInput(
            value = state.intendedMeaningEs,
            onValueChange = {
                onIntent(NewCardUiIntent.IntendedMeaningChanged(it))
            },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.intended_meaning_label),
            placeholder = stringResource(R.string.intended_meaning_placeholder),
            supportingText = stringResource(R.string.intended_meaning_supporting_text),
        )
        HInput(
            value = state.contextSentence,
            onValueChange = {
                onIntent(NewCardUiIntent.ContextSentenceChanged(it))
            },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.context_sentence_label),
            placeholder = stringResource(R.string.context_sentence_placeholder),
            supportingText = stringResource(R.string.context_sentence_supporting_text),
        )
    }
}

@Composable
private fun CategoryInputSection(
    state: NewCardUiState,
    onIntent: (NewCardUiIntent) -> Unit,
    onShowCategoryPicker: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        JustClickableInput(
            value = state.category.name,
            label = stringResource(R.string.category_label),
            onClick = onShowCategoryPicker,
        )
        SupportingText(text = stringResource(R.string.category_supporting_text))
        HSelect(
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.difficulty_label),
            items = difficult,
            itemSelected = state.difficulty,
            onItemSelected = { onIntent(NewCardUiIntent.DifficultySelected(it)) },
        )
        SupportingText(text = stringResource(R.string.difficulty_supporting_text))
    }
}

@Composable
private fun LoadingPreviewSkeleton() {
    HCard(variant = CardVariant.Outlined) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            HBadge(
                label = stringResource(R.string.loading_preview_badge),
                variant = BadgeVariant.Secondary,
            )
            Text(
                text = stringResource(R.string.loading_preview_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.loading_preview_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HSeparator()
            LoadingStepSkeleton(
                title = stringResource(R.string.loading_step_understanding_input),
                lines = 2,
            )
            LoadingStepSkeleton(
                title = stringResource(R.string.loading_step_building_note),
                lines = 3,
            )
            LoadingStepSkeleton(
                title = stringResource(R.string.loading_step_building_cards),
                lines = 2,
            )
        }
    }
}

@Composable
private fun ResultPreviewSection(
    state: NewCardUiState,
    keyboardController: SoftwareKeyboardController?,
    onIntent: (NewCardUiIntent) -> Unit,
) {
    val learningNotePreview = state.learningNotePreview ?: return

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(RESULT_FADE_IN_DURATION_MS)),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            HAlert(
                title = stringResource(R.string.preview_summary_title, learningNotePreview.cards.size),
                description = stringResource(R.string.preview_summary_description),
                variant = AlertVariant.Default,
            )
            Text(
                stringResource(R.string.verify_result_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            LearningNotePreview(
                note = learningNotePreview,
                validationIssues = state.previewValidationIssues,
                warningIssues = state.previewWarningIssues,
                noteRegenerationTarget = state.previewRegenerationTarget,
                onIntent = onIntent,
            )

            HButton(
                text = stringResource(R.string.save_in_deck, state.deckSelected?.name.orEmpty()),
                onClick = {
                    keyboardController?.hide()
                    onIntent(NewCardUiIntent.SaveClicked)
                },
                enabled = !state.isLoading && state.canSavePreview,
                isLoading = state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            )

            if (state.previewValidationErrors.isNotEmpty()) {
                HAlert(
                    title = stringResource(R.string.preview_not_saveable_title),
                    description = state.previewValidationErrors.joinToString(separator = "\n"),
                    variant = AlertVariant.Destructive,
                )
            }

            if (state.previewWarnings.isNotEmpty()) {
                HAlert(
                    title = stringResource(R.string.preview_warnings_title),
                    description = if (state.previewWarningIssues.isNotEmpty()) {
                        "Revisa los warnings resaltados dentro del preview antes de guardar."
                    } else {
                        state.previewWarnings.joinToString(separator = "\n")
                    },
                    variant = AlertVariant.Warning,
                )
            }
        }
    }
}

@Composable
private fun LearningNotePreview(
    note: GeneratedLearningNote,
    validationIssues: List<GeneratedLearningNoteIssue>,
    warningIssues: List<GeneratedLearningNoteIssue>,
    noteRegenerationTarget: PreviewRegenerationTarget?,
    onIntent: (NewCardUiIntent) -> Unit,
) {
    HCard(variant = CardVariant.Outlined) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PreviewOverview(note = note)

            HSeparator()
            PreviewSectionHeader(
                step = stringResource(R.string.preview_step_note_badge),
                title = stringResource(R.string.preview_step_note_title),
                description = stringResource(R.string.preview_step_note_description),
            )

            EditablePreviewField(
                label = stringResource(R.string.translation_label),
                value = note.intendedMeaningEs,
                placeholder = "Significado intencional en espanol",
                errorMessage = validationIssues.noteFieldMessage("intendedMeaningEs"),
                supportingText = warningIssues.noteFieldMessage("intendedMeaningEs"),
                onValueChange = {
                    onIntent(
                        NewCardUiIntent.PreviewFieldChanged(
                            field = EditableLearningNoteField.IntendedMeaningEs,
                            value = it,
                        )
                    )
                },
            )
            EditablePreviewField(
                label = stringResource(R.string.meaning_label),
                value = note.simpleDefinitionEn,
                placeholder = "Define el significado en ingles simple",
                minLines = 2,
                errorMessage = validationIssues.noteFieldMessage("simpleDefinitionEn"),
                supportingText = warningIssues.noteFieldMessage("simpleDefinitionEn"),
                onValueChange = {
                    onIntent(
                        NewCardUiIntent.PreviewFieldChanged(
                            field = EditableLearningNoteField.SimpleDefinitionEn,
                            value = it,
                        )
                    )
                },
            )
            PreviewAlertGroup(alerts = note.meaningAlerts())
            EditablePreviewField(
                label = stringResource(R.string.why_useful_label),
                value = note.whyUseful,
                placeholder = "Por que vale la pena aprender esta nota",
                minLines = 2,
                errorMessage = validationIssues.noteFieldMessage("whyUseful"),
                supportingText = warningIssues.noteFieldMessage("whyUseful"),
                onValueChange = {
                    onIntent(
                        NewCardUiIntent.PreviewFieldChanged(
                            field = EditableLearningNoteField.WhyUseful,
                            value = it,
                        )
                    )
                },
            )
            RegenerateFieldButton(
                text = "Regenerar why useful",
                field = EditableLearningNoteField.WhyUseful,
                noteRegenerationTarget = noteRegenerationTarget,
                onIntent = onIntent,
            )

            if (note.usagePattern.isNotBlank()) {
                HSeparator()
                EditablePreviewField(
                    label = stringResource(R.string.usage_pattern_label),
                    value = note.usagePattern,
                    placeholder = "Patron de uso",
                    minLines = 2,
                    errorMessage = validationIssues.noteFieldMessage("usagePattern"),
                    supportingText = warningIssues.noteFieldMessage("usagePattern"),
                    onValueChange = {
                        onIntent(
                            NewCardUiIntent.PreviewFieldChanged(
                                field = EditableLearningNoteField.UsagePattern,
                                value = it,
                            )
                        )
                    },
                )
                RegenerateFieldButton(
                    text = "Regenerar usage pattern",
                    field = EditableLearningNoteField.UsagePattern,
                    noteRegenerationTarget = noteRegenerationTarget,
                    onIntent = onIntent,
                )
            }

            if (note.commonMistake.isNotBlank()) {
                EditablePreviewField(
                    label = stringResource(R.string.common_mistake_label),
                    value = note.commonMistake,
                    placeholder = "Error comun a evitar",
                    minLines = 2,
                    supportingText = warningIssues.noteFieldMessage("commonMistake"),
                    onValueChange = {
                        onIntent(
                            NewCardUiIntent.PreviewFieldChanged(
                                field = EditableLearningNoteField.CommonMistake,
                                value = it,
                            )
                        )
                    },
                )
                RegenerateFieldButton(
                    text = "Regenerar common mistake",
                    field = EditableLearningNoteField.CommonMistake,
                    noteRegenerationTarget = noteRegenerationTarget,
                    onIntent = onIntent,
                )
            }

            if (note.clozeSentence.isNotBlank()) {
                EditablePreviewField(
                    label = "Cloze",
                    value = note.clozeSentence,
                    placeholder = "Frase cloze",
                    minLines = 2,
                    errorMessage = validationIssues.noteFieldMessage("clozeSentence"),
                    supportingText = warningIssues.noteFieldMessage("clozeSentence"),
                    onValueChange = {
                        onIntent(
                            NewCardUiIntent.PreviewFieldChanged(
                                field = EditableLearningNoteField.ClozeSentence,
                                value = it,
                            )
                        )
                    },
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    HButton(
                        text = "Regenerar cloze",
                        onClick = { onIntent(NewCardUiIntent.RegenerateClozeClicked) },
                        variant = ButtonVariant.Ghost,
                        isLoading = noteRegenerationTarget == PreviewRegenerationTarget.Cloze,
                        enabled = noteRegenerationTarget == null || noteRegenerationTarget == PreviewRegenerationTarget.Cloze,
                    )
                }
            }

            if (note.collocations.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    note.collocations.take(MAX_PREVIEW_COLLOCATIONS).forEach { collocation ->
                        HBadge(label = collocation, variant = BadgeVariant.Outline)
                    }
                }
            }

            HSeparator()
            PreviewSectionHeader(
                step = stringResource(R.string.preview_step_example_badge),
                title = stringResource(R.string.preview_step_example_title),
                description = stringResource(R.string.preview_step_example_description),
            )
            EditablePreviewField(
                label = "Example sentence",
                value = note.exampleSentence,
                placeholder = "Ejemplo principal",
                minLines = 2,
                errorMessage = validationIssues.noteFieldMessage("exampleSentence"),
                supportingText = warningIssues.noteFieldMessage("exampleSentence"),
                onValueChange = {
                    onIntent(
                        NewCardUiIntent.PreviewFieldChanged(
                            field = EditableLearningNoteField.ExampleSentence,
                            value = it,
                        )
                    )
                },
            )
            EditablePreviewField(
                label = "Example translation",
                value = note.exampleTranslation,
                placeholder = "Traduccion del ejemplo",
                minLines = 2,
                errorMessage = validationIssues.noteFieldMessage("exampleTranslation"),
                supportingText = warningIssues.noteFieldMessage("exampleTranslation"),
                onValueChange = {
                    onIntent(
                        NewCardUiIntent.PreviewFieldChanged(
                            field = EditableLearningNoteField.ExampleTranslation,
                            value = it,
                        )
                    )
                },
            )
            PreviewAlertGroup(alerts = note.exampleAlerts())
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                HButton(
                    text = "Regenerar ejemplo",
                    onClick = { onIntent(NewCardUiIntent.RegenerateExampleClicked) },
                    variant = ButtonVariant.Ghost,
                    isLoading = noteRegenerationTarget == PreviewRegenerationTarget.Example,
                    enabled = noteRegenerationTarget == null || noteRegenerationTarget == PreviewRegenerationTarget.Example,
                )
            }

            if (note.cards.isNotEmpty()) {
                HSeparator()
                PreviewSectionHeader(
                    step = stringResource(R.string.preview_step_cards_badge),
                    title = stringResource(R.string.preview_step_cards_title),
                    description = stringResource(R.string.preview_step_cards_description),
                )
                PreviewAlertGroup(alerts = note.cardSectionAlerts())
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    note.cards.forEachIndexed { index, card ->
                        key(card.cardId) {
                            GeneratedStudyCardItem(
                                index = index,
                                total = note.cards.size,
                                card = card,
                                validationIssues = validationIssues,
                                warningIssues = warningIssues,
                                regenerationTarget = noteRegenerationTarget,
                                onPromptChanged = {
                                    onIntent(
                                        NewCardUiIntent.PreviewCardPromptChanged(
                                            cardId = card.cardId,
                                            prompt = it,
                                        )
                                    )
                                },
                                onExpectedAnswerChanged = {
                                    onIntent(
                                        NewCardUiIntent.PreviewCardExpectedAnswerChanged(
                                            cardId = card.cardId,
                                            expectedAnswer = it,
                                        )
                                    )
                                },
                                onHintChanged = {
                                    onIntent(
                                        NewCardUiIntent.PreviewCardHintChanged(
                                            cardId = card.cardId,
                                            hint = it,
                                        )
                                    )
                                },
                                onActiveChanged = {
                                    onIntent(
                                        NewCardUiIntent.PreviewCardActiveChanged(
                                            cardId = card.cardId,
                                            isActive = it,
                                        )
                                    )
                                },
                                onRegenerate = {
                                    onIntent(NewCardUiIntent.RegenerateCardClicked(card.cardId))
                                },
                            )
                        }
                    }
                }
            }

            if (note.qualityChecks.isNotEmpty()) {
                HSeparator()
                Text(
                    text = stringResource(R.string.quality_checks_label),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    note.qualityChecks.forEach { check ->
                        HAlert(
                            title = if (check.passed) {
                                stringResource(R.string.quality_check_passed, check.code.name)
                            } else {
                                stringResource(R.string.quality_check_failed, check.code.name)
                            },
                            description = check.message,
                            variant = if (check.passed) AlertVariant.Success else AlertVariant.Destructive,
                        )
                    }
                }
            }

            if (note.warnings.isNotEmpty()) {
                HSeparator()
                Text(
                    text = stringResource(R.string.warnings_label),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    note.warnings.forEach { warning ->
                        HAlert(
                            title = stringResource(R.string.preview_warning_item_title),
                            description = warning,
                            variant = AlertVariant.Warning,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GeneratedStudyCardItem(
    index: Int,
    total: Int,
    card: GeneratedStudyCard,
    validationIssues: List<GeneratedLearningNoteIssue>,
    warningIssues: List<GeneratedLearningNoteIssue>,
    regenerationTarget: PreviewRegenerationTarget?,
    onPromptChanged: (String) -> Unit,
    onExpectedAnswerChanged: (String) -> Unit,
    onHintChanged: (String) -> Unit,
    onActiveChanged: (Boolean) -> Unit,
    onRegenerate: () -> Unit,
) {
    HCard(variant = CardVariant.Outlined) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.generated_card_title, index + 1, total),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = card.cardType.description(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HBadge(
                    label = if (card.isActive) {
                        stringResource(R.string.card_active_badge)
                    } else {
                        stringResource(R.string.card_inactive_badge)
                    },
                    variant = if (card.isActive) BadgeVariant.Success else BadgeVariant.Outline,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HBadge(label = card.cardType.displayName(), variant = BadgeVariant.Secondary)
                HBadge(label = card.evaluationMode.displayName(), variant = BadgeVariant.Outline)
            }

            if (card.explanation.isNotBlank()) {
                HAlert(
                    title = stringResource(R.string.card_explanation_title),
                    description = card.explanation,
                    variant = AlertVariant.Default,
                )
            }

            card.sourceField.takeIf(String::isNotBlank)?.let { sourceField ->
                InfoRow(
                    label = stringResource(R.string.card_source_label),
                    value = sourceField.sourceFieldDisplayName(),
                )
            }

            EditablePreviewField(
                label = stringResource(R.string.card_front_label),
                value = card.prompt,
                placeholder = stringResource(R.string.card_front_placeholder),
                minLines = 2,
                errorMessage = validationIssues.cardMessage(card.cardId, isAnswer = false),
                helperText = stringResource(R.string.card_front_supporting_text),
                supportingText = warningIssues.cardWarning(card.cardId),
                onValueChange = onPromptChanged,
            )
            EditablePreviewField(
                label = stringResource(R.string.card_answer_label),
                value = card.expectedAnswer,
                placeholder = stringResource(R.string.card_answer_placeholder),
                minLines = 2,
                errorMessage = validationIssues.cardMessage(card.cardId, isAnswer = true),
                helperText = stringResource(R.string.card_answer_supporting_text),
                onValueChange = onExpectedAnswerChanged,
            )
            EditablePreviewField(
                label = stringResource(R.string.card_hint_label),
                value = card.hint,
                placeholder = stringResource(R.string.card_hint_placeholder),
                minLines = 2,
                helperText = stringResource(R.string.card_hint_supporting_text),
                onValueChange = onHintChanged,
            )
            LabeledCheckbox(
                label = "Incluir esta card en study",
                checked = card.isActive,
                isEnabled = true,
                onCheckedChange = onActiveChanged,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                HButton(
                    text = "Regenerar card",
                    onClick = onRegenerate,
                    variant = ButtonVariant.Ghost,
                    isLoading = regenerationTarget == PreviewRegenerationTarget.Card(card.cardId),
                    enabled = regenerationTarget == null || regenerationTarget == PreviewRegenerationTarget.Card(card.cardId),
                )
            }
        }
    }
}

@Composable
private fun PreviewAlertGroup(alerts: List<PreviewAlertModel>) {
    if (alerts.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        alerts.forEach { alert ->
            HAlert(
                title = alert.title,
                description = alert.description,
                variant = alert.variant,
            )
        }
    }
}

@Composable
private fun EditablePreviewField(
    label: String,
    value: String,
    placeholder: String,
    minLines: Int = 1,
    helperText: String? = null,
    errorMessage: String? = null,
    supportingText: String? = null,
    onValueChange: (String) -> Unit,
) {
    HInput(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        errorMessage = errorMessage,
        supportingText = mergeSupportingTexts(helperText, supportingText),
        singleLine = minLines == 1,
        minLines = minLines,
        maxLines = if (minLines == 1) 1 else 4,
    )
}

@Composable
private fun RegenerateFieldButton(
    text: String,
    field: EditableLearningNoteField,
    noteRegenerationTarget: PreviewRegenerationTarget?,
    onIntent: (NewCardUiIntent) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        HButton(
            text = text,
            onClick = { onIntent(NewCardUiIntent.RegenerateFieldClicked(field)) },
            variant = ButtonVariant.Ghost,
            isLoading = noteRegenerationTarget == PreviewRegenerationTarget.Field(field),
            enabled = noteRegenerationTarget == null || noteRegenerationTarget == PreviewRegenerationTarget.Field(field),
        )
    }
}

private fun List<GeneratedLearningNoteIssue>.noteFieldMessage(noteField: String): String? {
    return firstOrNull { it.noteField == noteField }?.message
}

private fun List<GeneratedLearningNoteIssue>.cardMessage(cardId: String, isAnswer: Boolean): String? {
    val expectedCode = if (isAnswer) {
        com.emm.domain.flashcard.GeneratedLearningNoteIssueCode.EmptyCardAnswer
    } else {
        com.emm.domain.flashcard.GeneratedLearningNoteIssueCode.EmptyCardPrompt
    }
    return firstOrNull { it.cardId == cardId && it.code == expectedCode }?.message
}

private fun List<GeneratedLearningNoteIssue>.cardWarning(cardId: String): String? {
    return firstOrNull {
        it.cardId == cardId && it.code == com.emm.domain.flashcard.GeneratedLearningNoteIssueCode.InactiveCard
    }?.message
}

private fun mergeSupportingTexts(vararg values: String?): String? {
    val lines = values.filterNot { it.isNullOrBlank() }
    return if (lines.isEmpty()) null else lines.joinToString(separator = "\n")
}

private fun GeneratedLearningNote.meaningAlerts(): List<PreviewAlertModel> {
    return qualityChecks.failedAlertsFor(
        GeneratedNoteQualityCode.SingleMeaning,
        GeneratedNoteQualityCode.RequiredFieldsPresent,
    )
}

private fun GeneratedLearningNote.exampleAlerts(): List<PreviewAlertModel> {
    return qualityChecks.failedAlertsFor(
        GeneratedNoteQualityCode.NaturalExample,
        GeneratedNoteQualityCode.ExampleSupportsMeaning,
    )
}

private fun GeneratedLearningNote.cardSectionAlerts(): List<PreviewAlertModel> {
    return qualityChecks.failedAlertsFor(
        GeneratedNoteQualityCode.ClearCardFocus,
        GeneratedNoteQualityCode.NonAmbiguousAnswers,
        GeneratedNoteQualityCode.NoteCardAlignment,
    )
}

private fun List<GeneratedNoteQualityCheck>.failedAlertsFor(
    vararg codes: GeneratedNoteQualityCode,
): List<PreviewAlertModel> {
    val expectedCodes = codes.toSet()
    return filter { !it.passed && it.code in expectedCodes }
        .map { check ->
            PreviewAlertModel(
                title = check.code.toAlertTitle(),
                description = check.message,
                variant = AlertVariant.Warning,
            )
        }
}

private fun GeneratedNoteQualityCode.toAlertTitle(): String {
    return when (this) {
        GeneratedNoteQualityCode.SingleMeaning -> "Revisa el significado"
        GeneratedNoteQualityCode.NaturalExample -> "Revisa el ejemplo"
        GeneratedNoteQualityCode.ExampleSupportsMeaning -> "Ajusta ejemplo y significado"
        GeneratedNoteQualityCode.NonAmbiguousAnswers -> "Aclara la respuesta esperada"
        GeneratedNoteQualityCode.RequiredFieldsPresent -> "Completa la nota"
        GeneratedNoteQualityCode.ClearCardFocus -> "Enfoca mejor la card"
        GeneratedNoteQualityCode.NoteCardAlignment -> "Alinea la card con la nota"
    }
}

@Composable
private fun PreviewOverview(note: GeneratedLearningNote) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = note.expression,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            HBadge(
                label = note.noteType.displayName(),
                variant = BadgeVariant.Secondary,
            )
        }

        if (note.ipa.isNotBlank()) {
            Text(
                text = note.ipa,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        InfoRow(
            label = stringResource(R.string.preview_overview_note_label),
            value = "${note.noteType.displayName()} · ${note.partOfSpeech.displayName()}",
        )
        InfoRow(
            label = stringResource(R.string.preview_overview_focus_label),
            value = "${note.levelBand.displayName()} · ${note.domain.displayName()} · ${note.register.displayName()}",
        )
        InfoRow(
            label = stringResource(R.string.preview_overview_cards_label),
            value = "${note.cards.count { it.isActive }} activas de ${note.cards.size}",
        )
    }
}

@Composable
private fun PreviewSectionHeader(
    step: String,
    title: String,
    description: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        HBadge(label = step, variant = BadgeVariant.Outline)
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LoadingStepSkeleton(
    title: String,
    lines: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        repeat(lines) { index ->
            HSkeleton(
                Modifier
                    .fillMaxWidth(if (index == lines - 1) SKELETON_DETAIL_WIDTH else 1f)
                    .height(14.dp)
            )
        }
    }
}

// -- Voice Components ---------------------------------------------------------

@Composable
private fun VoiceInputButton(
    isListening: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    IconButton(
        onClick = onClick,
        modifier = Modifier.scale(scale)
    ) {
        Icon(
            imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
            contentDescription = stringResource(R.string.voice_input_desc),
            tint = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
    }
}

// -- Internal Composables -----------------------------------------------------

@Composable
private fun LabeledCheckbox(
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
            .padding(8.dp)
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SupportingText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SectionCard(
    title: String,
    description: String? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        content()
        HSeparator()
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ExampleItem(index: Int, example: Example) {
    var showTranslation by remember(example.exampleId) { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "$index.",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = example.text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }

        AnimatedVisibility(visible = showTranslation) {
            Text(
                text = example.translation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp),
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            HButton(
                text = if (showTranslation) {
                    stringResource(
                        R.string.hide_translation
                    )
                } else {
                    stringResource(R.string.show_translation)
                },
                onClick = { showTranslation = !showTranslation },
                variant = ButtonVariant.Ghost,
            )
        }
    }
}

private fun LearningNoteType.displayName(): String {
    return when (this) {
        LearningNoteType.Word -> "Palabra"
        LearningNoteType.Phrase -> "Frase"
        LearningNoteType.PhrasalVerb -> "Phrasal verb"
        LearningNoteType.Idiom -> "Idiom"
        LearningNoteType.SentencePattern -> "Patrón"
    }
}

private fun PartOfSpeechTag.displayName(): String {
    return when (this) {
        PartOfSpeechTag.Noun -> "Sustantivo"
        PartOfSpeechTag.Verb -> "Verbo"
        PartOfSpeechTag.Adjective -> "Adjetivo"
        PartOfSpeechTag.Adverb -> "Adverbio"
        PartOfSpeechTag.Preposition -> "Preposición"
        PartOfSpeechTag.Conjunction -> "Conjunción"
        PartOfSpeechTag.Interjection -> "Interjección"
        PartOfSpeechTag.PhrasalVerb -> "Phrasal verb"
        PartOfSpeechTag.Idiom -> "Idiom"
        PartOfSpeechTag.Chunk -> "Chunk"
        PartOfSpeechTag.Other -> "Otro"
    }
}

private fun RegisterPreference.displayName(): String {
    return when (this) {
        RegisterPreference.Casual -> "Casual"
        RegisterPreference.Neutral -> "Neutral"
        RegisterPreference.Formal -> "Formal"
    }
}

private fun LevelBand.displayName(): String {
    return when (this) {
        LevelBand.A1_A2 -> "A1-A2"
        LevelBand.B1_B2 -> "B1-B2"
        LevelBand.C1_PLUS -> "C1+"
    }
}

private fun LearningDomain.displayName(): String {
    return when (this) {
        LearningDomain.DailyLife -> "Vida diaria"
        LearningDomain.Travel -> "Viajes"
        LearningDomain.Social -> "Social"
        LearningDomain.Work -> "Trabajo"
        LearningDomain.Study -> "Estudio"
        LearningDomain.Media -> "Medios"
        LearningDomain.Mixed -> "Mixto"
    }
}

private fun StudyCardType.displayName(): String {
    return when (this) {
        StudyCardType.Recognition -> "Reconocimiento"
        StudyCardType.Production -> "Producción"
        StudyCardType.Cloze -> "Cloze"
        StudyCardType.Form -> "Forma"
    }
}

private fun StudyCardType.description(): String {
    return when (this) {
        StudyCardType.Recognition -> "Sirve para reconocer el significado o uso correcto."
        StudyCardType.Production -> "Sirve para producir la expresión por tu cuenta."
        StudyCardType.Cloze -> "Sirve para completar una frase con la forma correcta."
        StudyCardType.Form -> "Sirve para fijarte en la forma exacta de la expresión."
    }
}

private fun EvaluationMode.displayName(): String {
    return when (this) {
        EvaluationMode.Exact -> "Respuesta exacta"
        EvaluationMode.FlexibleText -> "Texto flexible"
        EvaluationMode.ManualSelfCheck -> "Autoevaluación"
    }
}

private fun String.sourceFieldDisplayName(): String {
    return when (this) {
        "intendedMeaningEs" -> "la traducción objetivo"
        "simpleDefinitionEn" -> "la definición principal"
        "whyUseful" -> "la explicación de uso"
        "exampleSentence" -> "el ejemplo principal"
        "exampleTranslation" -> "la traducción del ejemplo"
        "usagePattern" -> "el patrón de uso"
        "commonMistake" -> "el error común"
        "clozeSentence" -> "la frase cloze"
        else -> this
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
                        id = "1",
                        name = "Vocabulario Inglés",
                        description = "",
                        createdAt = LocalDateTime.now(),
                        cards = listOf(),
                        cardsCount = 24,
                    )
                )
            )
        )
    }
}
