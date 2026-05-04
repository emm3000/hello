package com.emm.hello.newfeatures.card

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.emm.data.catalog.difficult
import com.emm.hello.R
import com.emm.hello.core.theme.spacing
import com.emm.hello.core.ui.BadgeVariant
import com.emm.hello.core.ui.CardVariant
import com.emm.hello.core.ui.HBadge
import com.emm.hello.core.ui.HCard
import com.emm.hello.core.ui.HInput
import com.emm.hello.core.ui.HSelect
import com.emm.hello.core.ui.HSelectTrigger

@Composable
internal fun NewCardInputSection(
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

        TypeView.WithAiHelp -> AiHelpInputSection(
            state = state,
            onIntent = onIntent,
        )
    }
}

@Composable
internal fun InputModeSelector(
    selectedMode: TypeView,
    onModeSelected: (TypeView) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)) {
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
        ModeOptionCard(
            title = stringResource(R.string.new_card_mode_ai),
            description = stringResource(R.string.mode_ai_description),
            helperText = stringResource(R.string.mode_ai_helper),
            isSelected = selectedMode == TypeView.WithAiHelp,
            onClick = { onModeSelected(TypeView.WithAiHelp) },
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
                .padding(MaterialTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
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
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)) {
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
                    onClick = onToggleVoiceInput,
                )
            },
        )
        HInput(
            value = state.intendedMeaningEs,
            onValueChange = { onIntent(NewCardUiIntent.IntendedMeaningChanged(it)) },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.intended_meaning_label),
            placeholder = stringResource(R.string.intended_meaning_placeholder),
            supportingText = stringResource(R.string.intended_meaning_supporting_text),
        )
        HInput(
            value = state.contextSentence,
            onValueChange = { onIntent(NewCardUiIntent.ContextSentenceChanged(it)) },
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
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)) {
        HSelectTrigger(
            value = state.category.name,
            label = stringResource(R.string.category_label),
            onClick = onShowCategoryPicker,
            supportingText = stringResource(R.string.category_supporting_text),
        )
        HSelect(
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.difficulty_label),
            items = difficult,
            itemSelected = state.difficulty,
            onItemSelected = { onIntent(NewCardUiIntent.DifficultySelected(it)) },
            supportingText = stringResource(R.string.difficulty_supporting_text),
        )
    }
}

@Composable
private fun AiHelpInputSection(
    state: NewCardUiState,
    onIntent: (NewCardUiIntent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)) {
        HInput(
            value = state.aiRequest,
            onValueChange = { onIntent(NewCardUiIntent.AiRequestChanged(it)) },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.ai_request_label),
            placeholder = stringResource(R.string.ai_request_placeholder),
            supportingText = stringResource(R.string.ai_request_supporting_text),
        )
        HSelect(
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.difficulty_label),
            items = difficult,
            itemSelected = state.difficulty,
            onItemSelected = { onIntent(NewCardUiIntent.DifficultySelected(it)) },
            supportingText = stringResource(R.string.ai_difficulty_supporting_text),
        )
    }
}

@Composable
private fun VoiceInputButton(
    isListening: Boolean,
    onClick: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale",
    )

    IconButton(
        onClick = onClick,
        modifier = Modifier.scale(scale),
    ) {
        Icon(
            imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
            contentDescription = stringResource(R.string.voice_input_desc),
            tint = if (isListening) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
        )
    }
}
