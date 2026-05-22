package com.emm.hello.newfeatures.card

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.emm.domain.generation.GeneratedLearningNote
import com.emm.domain.validation.ValidationIssue
import com.emm.hello.R
import com.emm.hello.core.theme.emberAccent
import com.emm.hello.core.theme.emberBadSoft
import com.emm.hello.core.theme.emberBg
import com.emm.hello.core.theme.emberDivider
import com.emm.hello.core.theme.emberFaint
import com.emm.hello.core.theme.emberHint
import com.emm.hello.core.theme.emberMuted
import com.emm.hello.core.theme.emberOnBg
import com.emm.hello.core.theme.emberSurface
import com.emm.hello.core.theme.geist
import com.emm.hello.core.theme.geistMono
import com.emm.hello.core.theme.instrumentSerif
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HButtonSize
import com.emm.hello.core.ui.HButtonVariant
import com.emm.hello.core.ui.HSeparator
import com.emm.hello.core.ui.HSkeleton
import com.emm.hello.newfeatures.card.validation.IssueTextMapper
import com.emm.hello.newfeatures.card.validation.PreviewField
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val RESULT_FADE_IN_DURATION_MS = 300
private const val SKELETON_DETAIL_WIDTH = 0.45f
private const val SKELETON_MEDIUM_WIDTH = 0.78f
private const val PULSE_DOT_DURATION_MS = 900
private const val PULSE_DOT_STAGGER_MS = 180
private const val PULSE_DOT_MIN_ALPHA = 0.25f
private const val PULSE_DOT_MAX_ALPHA = 1f
private const val PULSE_DOT_COUNT = 3
private const val QUOTA_RESET_HOURS_THRESHOLD = 2L

@Composable
internal fun LoadingPreviewSkeleton(word: String) {
    val trimmedWord = word.trim()
    val title = if (trimmedWord.isNotEmpty()) {
        stringResource(R.string.loading_preview_thinking_for, trimmedWord)
    } else {
        stringResource(R.string.loading_preview_thinking_generic)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        PulsingDots()
        Text(
            text = title,
            fontFamily = instrumentSerif,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Normal,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            color = emberOnBg,
        )
        Text(
            text = stringResource(R.string.loading_preview_eta),
            fontFamily = geistMono,
            fontWeight = FontWeight.Normal,
            fontSize = 10.5.sp,
            letterSpacing = 0.12.em,
            color = emberFaint,
        )
        HSeparator()
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            HSkeleton(Modifier.fillMaxWidth().height(14.dp))
            HSkeleton(Modifier.fillMaxWidth(SKELETON_MEDIUM_WIDTH).height(14.dp))
            HSkeleton(Modifier.fillMaxWidth(SKELETON_DETAIL_WIDTH).height(14.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            HSkeleton(Modifier.fillMaxWidth().height(14.dp))
            HSkeleton(Modifier.fillMaxWidth(SKELETON_MEDIUM_WIDTH).height(14.dp))
        }
    }
}

@Composable
private fun PulsingDots() {
    val transition = rememberInfiniteTransition(label = "loading_pulse")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(PULSE_DOT_COUNT) { index ->
            val alpha by transition.animateFloat(
                initialValue = PULSE_DOT_MIN_ALPHA,
                targetValue = PULSE_DOT_MAX_ALPHA,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = PULSE_DOT_DURATION_MS, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * PULSE_DOT_STAGGER_MS),
                ),
                label = "loading_pulse_dot_$index",
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(alpha)
                    .background(emberAccent, CircleShape),
            )
        }
    }
}

@Composable
internal fun QuotaExceededState(
    word: String,
    resetAt: Instant?,
    onCreateManually: () -> Unit,
    onNotifyTomorrow: () -> Unit,
) {
    val trimmedWord = word.trim()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(emberBadSoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "!",
                fontFamily = instrumentSerif,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Normal,
                fontSize = 30.sp,
                color = emberOnBg,
            )
        }

        Text(
            text = stringResource(R.string.quota_error_title),
            fontFamily = instrumentSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 32.sp,
            lineHeight = 36.sp,
            letterSpacing = (-0.5).sp,
            color = emberOnBg,
        )

        Text(
            text = stringResource(R.string.quota_error_body),
            fontFamily = geist,
            fontWeight = FontWeight.Normal,
            fontSize = 13.5.sp,
            lineHeight = 19.sp,
            color = emberMuted,
        )

        if (trimmedWord.isNotEmpty()) {
            YourWordSurface(word = trimmedWord)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            HButton(
                text = stringResource(R.string.quota_error_create_manually),
                onClick = onCreateManually,
                variant = HButtonVariant.Accent,
                size = HButtonSize.Lg,
                full = true,
            )
            HButton(
                text = stringResource(R.string.quota_error_notify_tomorrow),
                onClick = onNotifyTomorrow,
                variant = HButtonVariant.Ghost,
                size = HButtonSize.Lg,
                full = true,
            )
        }

        Text(
            text = formatResetHint(resetAt),
            fontFamily = geistMono,
            fontWeight = FontWeight.Normal,
            fontSize = 10.5.sp,
            letterSpacing = 0.12.em,
            color = emberFaint,
        )
    }
}

@Composable
private fun YourWordSurface(word: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(emberSurface)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.quota_error_your_word_label),
            fontFamily = geistMono,
            fontWeight = FontWeight.Medium,
            fontSize = 10.5.sp,
            letterSpacing = 0.12.em,
            color = emberMuted,
        )
        Text(
            text = word,
            fontFamily = instrumentSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 28.sp,
            lineHeight = 32.sp,
            letterSpacing = (-0.3).sp,
            color = emberOnBg,
        )
    }
}

@Composable
private fun formatResetHint(resetAt: Instant?): String {
    if (resetAt == null) return stringResource(R.string.quota_error_reset_unknown)
    val now = Instant.now()
    val delta = Duration.between(now, resetAt)
    if (delta.isZero || delta.isNegative) return stringResource(R.string.quota_error_reset_unknown)
    val hours = delta.toHours()
    return if (hours >= QUOTA_RESET_HOURS_THRESHOLD) {
        val localTime = LocalTime.ofInstant(resetAt, ZoneId.systemDefault())
        val formatted = localTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        stringResource(R.string.quota_error_reset_at, formatted)
    } else {
        val minutes = delta.toMinutes().coerceAtLeast(1)
        stringResource(R.string.quota_error_reset_in, "$minutes min")
    }
}

@Composable
internal fun ResultPreviewSection(
    state: NewCardUiState,
    @Suppress("UNUSED_PARAMETER") keyboardController: SoftwareKeyboardController?,
    onIntent: (NewCardUiIntent) -> Unit,
) {
    val learningNotePreview = state.learningNotePreview ?: return

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(RESULT_FADE_IN_DURATION_MS)),
    ) {
        LearningNotePreview(
            note = learningNotePreview,
            validationIssues = state.previewValidationIssues,
            warningIssues = state.previewWarningIssues,
            noteRegenerationTarget = state.previewRegenerationTarget,
            onIntent = onIntent,
        )
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
    val selectedCard = remember(note.cards, selectedCardId) {
        note.cards.firstOrNull { it.cardId == selectedCardId }
    }

    Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
        WordHeader(note = note)

        SignificadoBlock(
            note = note,
            noteRegenerationTarget = noteRegenerationTarget,
            issueTextMapper = issueTextMapper,
            validationIssues = validationIssues,
            warningIssues = warningIssues,
            onIntent = onIntent,
        )

        EjemploBlock(
            note = note,
            noteRegenerationTarget = noteRegenerationTarget,
            issueTextMapper = issueTextMapper,
            validationIssues = validationIssues,
            warningIssues = warningIssues,
            onIntent = onIntent,
        )

        if (note.usagePattern.isNotBlank()) {
            UsagePatternBlock(
                note = note,
                noteRegenerationTarget = noteRegenerationTarget,
                issueTextMapper = issueTextMapper,
                validationIssues = validationIssues,
                warningIssues = warningIssues,
                onIntent = onIntent,
            )
        }

        if (note.commonMistake.isNotBlank()) {
            CommonMistakeBlock(
                note = note,
                noteRegenerationTarget = noteRegenerationTarget,
                onIntent = onIntent,
            )
        }

        if (note.clozeSentence.isNotBlank()) {
            ClozeBlock(
                note = note,
                noteRegenerationTarget = noteRegenerationTarget,
                issueTextMapper = issueTextMapper,
                validationIssues = validationIssues,
                warningIssues = warningIssues,
                onIntent = onIntent,
            )
        }

        HSeparator()

        StudyCardsSection(
            note = note,
            validationIssues = validationIssues,
            warningIssues = warningIssues,
            onEditCard = { selectedCardId = it },
            onIntent = onIntent,
            noteRegenerationTarget = noteRegenerationTarget,
        )
    }

    if (selectedCard != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedCardId = null },
            containerColor = emberBg,
        ) {
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
private fun WordHeader(note: GeneratedLearningNote) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${note.partOfSpeech.displayName()} · ${note.levelBand.displayName()}",
                fontFamily = geistMono,
                fontWeight = FontWeight.Medium,
                fontSize = 10.5.sp,
                letterSpacing = 0.12.em,
                color = emberMuted,
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            text = note.expression.value.ifBlank { "—" },
            fontFamily = instrumentSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 54.sp,
            lineHeight = 58.sp,
            letterSpacing = (-0.5).sp,
            color = emberOnBg,
        )

        if (note.ipa.isNotBlank()) {
            Text(
                text = note.ipa,
                fontFamily = geistMono,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                color = emberHint,
            )
        }

        if (note.intendedMeaningEs.value.isNotBlank()) {
            Text(
                text = note.intendedMeaningEs.value,
                fontFamily = instrumentSerif,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Normal,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                color = emberOnBg,
            )
        }
    }
}

@Composable
private fun SignificadoBlock(
    note: GeneratedLearningNote,
    noteRegenerationTarget: PreviewRegenerationTarget?,
    issueTextMapper: IssueTextMapper,
    validationIssues: List<ValidationIssue>,
    warningIssues: List<ValidationIssue>,
    onIntent: (NewCardUiIntent) -> Unit,
) {
    val isRegen = noteRegenerationTarget ==
        PreviewRegenerationTarget.Field(EditableLearningNoteField.WhyUseful)
    val canRegen = noteRegenerationTarget == null || isRegen
    ReviewBlock(
        label = "Significado",
        value = note.whyUseful,
        placeholder = "Por qué vale la pena aprenderlo",
        minLines = 2,
        onRegenerate = {
            onIntent(NewCardUiIntent.RegenerateFieldClicked(EditableLearningNoteField.WhyUseful))
        },
        regenerateContentDescription = stringResource(R.string.regenerate_why_useful),
        isRegenerating = isRegen,
        regenerateEnabled = canRegen,
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
    if (note.simpleDefinitionEn.value.isNotBlank()) {
        Spacer(Modifier.height(12.dp))
        Text(
            text = note.simpleDefinitionEn.value,
            fontFamily = instrumentSerif,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            color = emberMuted,
        )
    }
    PreviewAlertGroup(alerts = note.meaningAlerts())
}

@Composable
private fun EjemploBlock(
    note: GeneratedLearningNote,
    noteRegenerationTarget: PreviewRegenerationTarget?,
    issueTextMapper: IssueTextMapper,
    validationIssues: List<ValidationIssue>,
    warningIssues: List<ValidationIssue>,
    onIntent: (NewCardUiIntent) -> Unit,
) {
    val isRegen = noteRegenerationTarget == PreviewRegenerationTarget.Example
    val canRegen = noteRegenerationTarget == null || isRegen

    ReviewBlock(
        label = "Ejemplo",
        value = note.exampleSentence,
        placeholder = "Ejemplo principal",
        minLines = 2,
        onRegenerate = { onIntent(NewCardUiIntent.RegenerateExampleClicked) },
        regenerateContentDescription = stringResource(R.string.example_sentence_label),
        isRegenerating = isRegen,
        regenerateEnabled = canRegen,
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
    if (note.exampleTranslation.isNotBlank()) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = note.exampleTranslation,
            fontFamily = instrumentSerif,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            color = emberMuted,
        )
    }
    PreviewAlertGroup(alerts = note.exampleAlerts())
}

@Composable
private fun UsagePatternBlock(
    note: GeneratedLearningNote,
    noteRegenerationTarget: PreviewRegenerationTarget?,
    issueTextMapper: IssueTextMapper,
    validationIssues: List<ValidationIssue>,
    warningIssues: List<ValidationIssue>,
    onIntent: (NewCardUiIntent) -> Unit,
) {
    val isRegen = noteRegenerationTarget ==
        PreviewRegenerationTarget.Field(EditableLearningNoteField.UsagePattern)
    val canRegen = noteRegenerationTarget == null || isRegen
    ReviewBlock(
        label = "Patrón de uso",
        value = note.usagePattern,
        placeholder = "Patrón de uso",
        minLines = 2,
        onRegenerate = {
            onIntent(NewCardUiIntent.RegenerateFieldClicked(EditableLearningNoteField.UsagePattern))
        },
        regenerateContentDescription = stringResource(R.string.regenerate_usage_pattern),
        isRegenerating = isRegen,
        regenerateEnabled = canRegen,
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
}

@Composable
private fun CommonMistakeBlock(
    note: GeneratedLearningNote,
    noteRegenerationTarget: PreviewRegenerationTarget?,
    onIntent: (NewCardUiIntent) -> Unit,
) {
    val isRegen = noteRegenerationTarget ==
        PreviewRegenerationTarget.Field(EditableLearningNoteField.CommonMistake)
    val canRegen = noteRegenerationTarget == null || isRegen
    ReviewBlock(
        label = "Error común",
        value = note.commonMistake,
        placeholder = "Error a evitar",
        minLines = 2,
        warn = true,
        onRegenerate = {
            onIntent(
                NewCardUiIntent.RegenerateFieldClicked(EditableLearningNoteField.CommonMistake),
            )
        },
        regenerateContentDescription = stringResource(R.string.regenerate_common_mistake),
        isRegenerating = isRegen,
        regenerateEnabled = canRegen,
        onValueChange = {
            onIntent(
                NewCardUiIntent.PreviewFieldChanged(
                    field = EditableLearningNoteField.CommonMistake,
                    value = it,
                ),
            )
        },
    )
}

@Composable
private fun ClozeBlock(
    note: GeneratedLearningNote,
    noteRegenerationTarget: PreviewRegenerationTarget?,
    issueTextMapper: IssueTextMapper,
    validationIssues: List<ValidationIssue>,
    warningIssues: List<ValidationIssue>,
    onIntent: (NewCardUiIntent) -> Unit,
) {
    val isRegen = noteRegenerationTarget == PreviewRegenerationTarget.Cloze
    val canRegen = noteRegenerationTarget == null || isRegen
    ReviewBlock(
        label = "Cloze",
        value = note.clozeSentence,
        placeholder = "Frase cloze",
        minLines = 2,
        onRegenerate = { onIntent(NewCardUiIntent.RegenerateClozeClicked) },
        regenerateContentDescription = "Regenerar cloze",
        isRegenerating = isRegen,
        regenerateEnabled = canRegen,
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
}

@Composable
private fun StudyCardsSection(
    note: GeneratedLearningNote,
    validationIssues: List<ValidationIssue>,
    warningIssues: List<ValidationIssue>,
    onEditCard: (String) -> Unit,
    onIntent: (NewCardUiIntent) -> Unit,
    noteRegenerationTarget: PreviewRegenerationTarget?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "TARJETAS DE ESTUDIO · ${note.cards.size}",
                fontFamily = geistMono,
                fontWeight = FontWeight.Medium,
                fontSize = 10.5.sp,
                letterSpacing = 0.12.em,
                color = emberMuted,
                modifier = Modifier.weight(1f),
            )
            val activeCount = note.cards.count { it.isActive }
            Text(
                text = "$activeCount activas",
                fontFamily = geistMono,
                fontSize = 10.5.sp,
                letterSpacing = 0.12.em,
                color = emberFaint,
            )
        }

        PreviewAlertGroup(alerts = note.cardSectionAlerts())

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(emberSurface),
        ) {
            note.cards.forEachIndexed { index, card ->
                key(card.cardId) {
                    StudyCardRow(
                        index = index,
                        total = note.cards.size,
                        card = card,
                        validationIssues = validationIssues,
                        warningIssues = warningIssues,
                        isRegenerating = noteRegenerationTarget == PreviewRegenerationTarget.Card(card.cardId),
                        regenerateEnabled = noteRegenerationTarget == null ||
                            noteRegenerationTarget == PreviewRegenerationTarget.Card(card.cardId),
                        onRegenerate = { onIntent(NewCardUiIntent.RegenerateCardClicked(card.cardId)) },
                        onToggleActive = {
                            onIntent(
                                NewCardUiIntent.PreviewCardActiveChanged(
                                    cardId = card.cardId,
                                    isActive = it,
                                ),
                            )
                        },
                        onEdit = { onEditCard(card.cardId) },
                    )
                    if (index < note.cards.lastIndex) {
                        HSeparator(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = emberDivider,
                        )
                    }
                }
            }
        }
    }
}
