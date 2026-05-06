package com.emm.hello.newfeatures.card

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emm.domain.generation.GeneratedLearningNote
import com.emm.domain.generation.GeneratedNoteQualityCheck
import com.emm.domain.validation.ValidationIssue
import com.emm.hello.R
import com.emm.hello.core.ui.AlertVariant
import com.emm.hello.core.ui.BadgeVariant
import com.emm.hello.core.ui.ButtonVariant
import com.emm.hello.core.ui.CardVariant
import com.emm.hello.core.ui.HAlert
import com.emm.hello.core.ui.HBadge
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HCard
import com.emm.hello.core.ui.HSeparator
import com.emm.hello.newfeatures.card.validation.IssueTextMapper
import com.emm.hello.newfeatures.card.validation.PreviewField

private const val RESULT_FADE_IN_DURATION_MS = 300

@Composable
internal fun LoadingPreviewSkeleton() {
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
internal fun ResultPreviewSection(
    state: NewCardUiState,
    keyboardController: SoftwareKeyboardController?,
    onIntent: (NewCardUiIntent) -> Unit,
) {
    val learningNotePreview = state.learningNotePreview ?: return
    val issueTextMapper = IssueTextMapper()
    val previewErrors = state.previewValidationIssues.localized(issueTextMapper)
    val previewWarnings = state.previewWarningIssues.localized(issueTextMapper) + state.previewGeneratedWarnings

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
            ReviewStatusSummary(
                validationErrors = previewErrors,
                warnings = previewWarnings,
                totalCards = learningNotePreview.cards.size,
                activeCards = learningNotePreview.cards.count { it.isActive },
            )
            LearningNotePreview(
                note = learningNotePreview,
                validationIssues = state.previewValidationIssues,
                warningIssues = state.previewWarningIssues,
                noteRegenerationTarget = state.previewRegenerationTarget,
                onIntent = onIntent,
            )

            if (!state.canSavePreview && previewErrors.isNotEmpty()) {
                SupportingText(
                    text = stringResource(
                        R.string.save_blocked_summary,
                        previewErrors.size,
                    ),
                )
            }
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

            if (previewErrors.isNotEmpty()) {
                HAlert(
                    title = stringResource(R.string.preview_not_saveable_title),
                    description = previewErrors.joinToString(separator = "\n"),
                    variant = AlertVariant.Destructive,
                )
            }

            if (previewWarnings.isNotEmpty()) {
                HAlert(
                    title = stringResource(R.string.preview_warnings_title),
                    description = if (state.previewWarningIssues.isNotEmpty()) {
                        stringResource(R.string.preview_warnings_inline_summary)
                    } else {
                        previewWarnings.joinToString(separator = "\n")
                    },
                    variant = AlertVariant.Warning,
                )
            }
        }
    }
}

@Composable
private fun ReviewStatusSummary(
    validationErrors: List<String>,
    warnings: List<String>,
    totalCards: Int,
    activeCards: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HBadge(
                label = stringResource(R.string.review_active_cards_badge, activeCards, totalCards),
                variant = BadgeVariant.Secondary,
            )
            if (validationErrors.isNotEmpty()) {
                HBadge(
                    label = stringResource(R.string.review_error_badge, validationErrors.size),
                    variant = BadgeVariant.Destructive,
                )
            }
            if (warnings.isNotEmpty()) {
                HBadge(
                    label = stringResource(R.string.review_warning_badge, warnings.size),
                    variant = BadgeVariant.Outline,
                )
            }
        }

        when {
            validationErrors.isNotEmpty() -> HAlert(
                title = stringResource(R.string.review_status_fix_title),
                description = validationErrors.first(),
                variant = AlertVariant.Destructive,
            )
            warnings.isNotEmpty() -> HAlert(
                title = stringResource(R.string.review_status_warning_title),
                description = warnings.first(),
                variant = AlertVariant.Warning,
            )
            else -> HAlert(
                title = stringResource(R.string.review_status_ready_title),
                description = stringResource(R.string.review_status_ready_description),
                variant = AlertVariant.Success,
            )
        }
    }
}

@Composable
private fun LearningNotePreview(
    note: GeneratedLearningNote,
    validationIssues: List<ValidationIssue>,
    warningIssues: List<ValidationIssue>,
    noteRegenerationTarget: PreviewRegenerationTarget?,
    onIntent: (NewCardUiIntent) -> Unit,
) {
    val issueTextMapper = IssueTextMapper()
    var selectedCardId by remember(note.noteId) { mutableStateOf<String?>(null) }
    var showPassedChecks by remember(note.noteId) { mutableStateOf(false) }
    var noteSectionExpanded by remember(note.noteId) { mutableStateOf(true) }
    var exampleSectionExpanded by remember(note.noteId) { mutableStateOf(false) }
    var cardsSectionExpanded by remember(note.noteId) { mutableStateOf(false) }
    val selectedCard = remember(note.cards, selectedCardId) {
        note.cards.firstOrNull { it.cardId == selectedCardId }
    }
    val failedChecks = remember(note.qualityChecks) {
        note.qualityChecks.filterNot(GeneratedNoteQualityCheck::passed)
    }
    val passedChecks = remember(note.qualityChecks) {
        note.qualityChecks.filter(GeneratedNoteQualityCheck::passed)
    }

    HCard(variant = CardVariant.Outlined) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PreviewOverview(note = note)

            HSeparator()
            CollapsiblePreviewSection(
                step = stringResource(R.string.preview_step_note_badge),
                title = stringResource(R.string.preview_step_note_title),
                description = stringResource(R.string.preview_step_note_description),
                collapsedSummary = note.noteSectionSummary(),
                expanded = noteSectionExpanded,
                onExpandedChange = { noteSectionExpanded = it },
            ) {
                EditablePreviewField(
                    label = stringResource(R.string.translation_label),
                    value = note.intendedMeaningEs.value,
                    placeholder = "Significado intencional en espanol",
                    errorMessage = validationIssues.noteFieldMessage(PreviewField.IntendedMeaning, issueTextMapper),
                    supportingText = warningIssues.noteFieldMessage(PreviewField.IntendedMeaning, issueTextMapper),
                    onValueChange = {
                        onIntent(
                            NewCardUiIntent.PreviewFieldChanged(
                                field = EditableLearningNoteField.IntendedMeaningEs,
                                value = it,
                            ),
                        )
                    },
                )
                EditablePreviewField(
                    label = stringResource(R.string.meaning_label),
                    value = note.simpleDefinitionEn.value,
                    placeholder = "Define el significado en ingles simple",
                    minLines = 2,
                    errorMessage = validationIssues.noteFieldMessage(PreviewField.SimpleDefinition, issueTextMapper),
                    supportingText = warningIssues.noteFieldMessage(PreviewField.SimpleDefinition, issueTextMapper),
                    onValueChange = {
                        onIntent(
                            NewCardUiIntent.PreviewFieldChanged(
                                field = EditableLearningNoteField.SimpleDefinitionEn,
                                value = it,
                            ),
                        )
                    },
                )
                PreviewAlertGroup(alerts = note.meaningAlerts())
                EditablePreviewField(
                    label = stringResource(R.string.why_useful_label),
                    value = note.whyUseful,
                    placeholder = "Por que vale la pena aprender esta nota",
                    minLines = 2,
                    errorMessage = validationIssues.noteFieldMessage(PreviewField.WhyUseful, issueTextMapper),
                    supportingText = warningIssues.noteFieldMessage(PreviewField.WhyUseful, issueTextMapper),
                    onValueChange = {
                        onIntent(
                            NewCardUiIntent.PreviewFieldChanged(
                                field = EditableLearningNoteField.WhyUseful,
                                value = it,
                            ),
                        )
                    },
                )
                RegenerateFieldButton(
                    text = stringResource(R.string.regenerate_why_useful),
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
                        errorMessage = validationIssues.noteFieldMessage(PreviewField.UsagePattern, issueTextMapper),
                        supportingText = warningIssues.noteFieldMessage(PreviewField.UsagePattern, issueTextMapper),
                        onValueChange = {
                            onIntent(
                                NewCardUiIntent.PreviewFieldChanged(
                                    field = EditableLearningNoteField.UsagePattern,
                                    value = it,
                                ),
                            )
                        },
                    )
                    RegenerateFieldButton(
                        text = stringResource(R.string.regenerate_usage_pattern),
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
                        onValueChange = {
                            onIntent(
                                NewCardUiIntent.PreviewFieldChanged(
                                    field = EditableLearningNoteField.CommonMistake,
                                    value = it,
                                ),
                            )
                        },
                    )
                    RegenerateFieldButton(
                        text = stringResource(R.string.regenerate_common_mistake),
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
                        errorMessage = validationIssues.noteFieldMessage(PreviewField.ClozeSentence, issueTextMapper),
                        supportingText = warningIssues.noteFieldMessage(PreviewField.ClozeSentence, issueTextMapper),
                        onValueChange = {
                            onIntent(
                                NewCardUiIntent.PreviewFieldChanged(
                                    field = EditableLearningNoteField.ClozeSentence,
                                    value = it,
                                ),
                            )
                        },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        HButton(
                            text = "Regenerar cloze",
                            onClick = { onIntent(NewCardUiIntent.RegenerateClozeClicked) },
                            variant = ButtonVariant.Ghost,
                            isLoading = noteRegenerationTarget == PreviewRegenerationTarget.Cloze,
                            enabled = noteRegenerationTarget == null ||
                                noteRegenerationTarget == PreviewRegenerationTarget.Cloze,
                        )
                    }
                }

                if (note.collocations.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        note.collocationsPreview().forEach { collocation ->
                            HBadge(label = collocation, variant = BadgeVariant.Outline)
                        }
                    }
                }
            }

            HSeparator()
            CollapsiblePreviewSection(
                step = stringResource(R.string.preview_step_example_badge),
                title = stringResource(R.string.preview_step_example_title),
                description = stringResource(R.string.preview_step_example_description),
                collapsedSummary = note.exampleSectionSummary(),
                expanded = exampleSectionExpanded,
                onExpandedChange = { exampleSectionExpanded = it },
            ) {
                EditablePreviewField(
                    label = stringResource(R.string.example_sentence_label),
                    value = note.exampleSentence,
                    placeholder = "Ejemplo principal",
                    minLines = 2,
                    errorMessage = validationIssues.noteFieldMessage(PreviewField.ExampleSentence, issueTextMapper),
                    supportingText = warningIssues.noteFieldMessage(PreviewField.ExampleSentence, issueTextMapper),
                    onValueChange = {
                        onIntent(
                            NewCardUiIntent.PreviewFieldChanged(
                                field = EditableLearningNoteField.ExampleSentence,
                                value = it,
                            ),
                        )
                    },
                )
                EditablePreviewField(
                    label = stringResource(R.string.example_translation_label),
                    value = note.exampleTranslation,
                    placeholder = "Traduccion del ejemplo",
                    minLines = 2,
                    errorMessage = validationIssues.noteFieldMessage(PreviewField.ExampleTranslation, issueTextMapper),
                    supportingText = warningIssues.noteFieldMessage(PreviewField.ExampleTranslation, issueTextMapper),
                    onValueChange = {
                        onIntent(
                            NewCardUiIntent.PreviewFieldChanged(
                                field = EditableLearningNoteField.ExampleTranslation,
                                value = it,
                            ),
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
                        enabled = noteRegenerationTarget == null ||
                            noteRegenerationTarget == PreviewRegenerationTarget.Example,
                    )
                }
            }

            if (note.cards.isNotEmpty()) {
                HSeparator()
                CollapsiblePreviewSection(
                    step = stringResource(R.string.preview_step_cards_badge),
                    title = stringResource(R.string.preview_step_cards_title),
                    description = stringResource(R.string.preview_step_cards_description),
                    collapsedSummary = note.cardsSectionSummary(),
                    expanded = cardsSectionExpanded,
                    onExpandedChange = { cardsSectionExpanded = it },
                ) {
                    PreviewAlertGroup(alerts = note.cardSectionAlerts())
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        note.cards.forEachIndexed { index, card ->
                            key(card.cardId) {
                                GeneratedStudyCardSummaryItem(
                                    index = index,
                                    total = note.cards.size,
                                    card = card,
                                    validationIssues = validationIssues,
                                    warningIssues = warningIssues,
                                    onClick = { selectedCardId = card.cardId },
                                )
                            }
                        }
                    }
                }
            }

            if (failedChecks.isNotEmpty() || passedChecks.isNotEmpty()) {
                HSeparator()
                Text(
                    text = stringResource(R.string.quality_checks_label),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    failedChecks.forEach { check ->
                        HAlert(
                            title = stringResource(R.string.quality_check_failed, check.code.name),
                            description = check.message,
                            variant = AlertVariant.Destructive,
                        )
                    }

                    if (passedChecks.isNotEmpty()) {
                        HButton(
                            text = if (showPassedChecks) {
                                stringResource(R.string.hide_passed_checks, passedChecks.size)
                            } else {
                                stringResource(R.string.show_passed_checks, passedChecks.size)
                            },
                            onClick = { showPassedChecks = !showPassedChecks },
                            variant = ButtonVariant.Link,
                        )
                    }

                    if (showPassedChecks) {
                        passedChecks.forEach { check ->
                            HAlert(
                                title = stringResource(R.string.quality_check_passed, check.code.name),
                                description = check.message,
                                variant = AlertVariant.Success,
                            )
                        }
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

    if (selectedCard != null) {
        ModalBottomSheet(onDismissRequest = { selectedCardId = null }) {
            GeneratedStudyCardEditorSheet(
                card = selectedCard,
                validationIssues = validationIssues,
                warningIssues = warningIssues,
                regenerationTarget = noteRegenerationTarget,
                onPromptChanged = {
                    onIntent(
                        NewCardUiIntent.PreviewCardPromptChanged(
                            cardId = selectedCard.cardId,
                            prompt = it,
                        ),
                    )
                },
                onExpectedAnswerChanged = {
                    onIntent(
                        NewCardUiIntent.PreviewCardExpectedAnswerChanged(
                            cardId = selectedCard.cardId,
                            expectedAnswer = it,
                        ),
                    )
                },
                onHintChanged = {
                    onIntent(
                        NewCardUiIntent.PreviewCardHintChanged(
                            cardId = selectedCard.cardId,
                            hint = it,
                        ),
                    )
                },
                onActiveChanged = {
                    onIntent(
                        NewCardUiIntent.PreviewCardActiveChanged(
                            cardId = selectedCard.cardId,
                            isActive = it,
                        ),
                    )
                },
                onRegenerate = {
                    onIntent(NewCardUiIntent.RegenerateCardClicked(selectedCard.cardId))
                },
            )
        }
    }
}

@Composable
private fun List<ValidationIssue>.localized(issueTextMapper: IssueTextMapper): List<String> {
    return map { stringResource(issueTextMapper.map(it).textResId) }
}

@Composable
private fun CollapsiblePreviewSection(
    step: String,
    title: String,
    description: String,
    collapsedSummary: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandedChange(!expanded) },
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PreviewSectionHeader(
                step = step,
                title = title,
                description = description,
            )
            Text(
                text = if (expanded) {
                    stringResource(R.string.preview_section_hide)
                } else {
                    stringResource(R.string.preview_section_show)
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            if (!expanded) {
                Text(
                    text = collapsedSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                content()
            }
        }
    }
}
