package com.emm.hello.newfeatures.card

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.emm.domain.generation.GeneratedStudyCard
import com.emm.domain.validation.ValidationIssue
import com.emm.hello.R
import com.emm.hello.core.theme.ink
import com.emm.hello.core.theme.inkFaint
import com.emm.hello.core.theme.inkMuted
import com.emm.hello.core.theme.geistMono
import com.emm.hello.core.ui.AlertVariant
import com.emm.hello.core.ui.HAlert
import com.emm.hello.core.ui.HToggle
import com.emm.hello.newfeatures.card.validation.IssueTextMapper
import com.emm.hello.newfeatures.card.validation.PreviewCardField

private const val INACTIVE_ROW_ALPHA = 0.5f

@Composable
internal fun StudyCardRow(
    index: Int,
    total: Int,
    card: GeneratedStudyCard,
    validationIssues: List<ValidationIssue>,
    warningIssues: List<ValidationIssue>,
    isRegenerating: Boolean,
    regenerateEnabled: Boolean,
    onRegenerate: () -> Unit,
    onToggleActive: (Boolean) -> Unit,
    onEdit: () -> Unit,
) {
    val issueTextMapper = IssueTextMapper()
    val rowAlpha = if (card.isActive) 1f else INACTIVE_ROW_ALPHA

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HToggle(
            checked = card.isActive,
            onCheckedChange = onToggleActive,
        )
        Spacer(Modifier.width(14.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .alpha(rowAlpha),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "${index + 1}/$total · ${card.cardType.displayName().uppercase()}",
                fontFamily = geistMono,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                letterSpacing = 0.12.em,
                color = inkFaint,
            )
            if (card.prompt.isNotBlank()) {
                Text(
                    text = card.prompt,
                    fontWeight = FontWeight.Normal,
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                    color = ink,
                    maxLines = 2,
                )
            }
            if (card.expectedAnswer.isNotBlank()) {
                Text(
                    text = card.expectedAnswer,
                    fontWeight = FontWeight.Normal,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    color = inkMuted,
                    maxLines = 2,
                )
            }
            val errorPrompt = validationIssues.cardMessage(
                cardId = card.cardId,
                field = PreviewCardField.Prompt,
                issueTextMapper = issueTextMapper,
            )
            val errorAnswer = validationIssues.cardMessage(
                cardId = card.cardId,
                field = PreviewCardField.ExpectedAnswer,
                issueTextMapper = issueTextMapper,
            )
            val warning = warningIssues.cardWarning(card.cardId, issueTextMapper)
            val firstAlert = errorPrompt ?: errorAnswer ?: warning
            if (firstAlert != null) {
                Text(
                    text = firstAlert,
                    fontFamily = geistMono,
                    fontWeight = FontWeight.Normal,
                    fontSize = 10.5.sp,
                    letterSpacing = 0.08.em,
                    color = ink,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        ReviewBlockIconButton(
            icon = Icons.Outlined.Refresh,
            contentDescription = stringResource(R.string.regenerate_card_action),
            onClick = onRegenerate,
            enabled = regenerateEnabled && !isRegenerating,
        )
    }
}

@Composable
internal fun GeneratedStudyCardEditorSheet(
    card: GeneratedStudyCard,
    validationIssues: List<ValidationIssue>,
    warningIssues: List<ValidationIssue>,
    regenerationTarget: PreviewRegenerationTarget?,
    onPromptChanged: (String) -> Unit,
    onExpectedAnswerChanged: (String) -> Unit,
    onHintChanged: (String) -> Unit,
    onActiveChanged: (Boolean) -> Unit,
    onRegenerate: () -> Unit,
) {
    val issueTextMapper = IssueTextMapper()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "TARJETA · ${card.cardType.displayName().uppercase()}",
            fontFamily = geistMono,
            fontWeight = FontWeight.Medium,
            fontSize = 10.5.sp,
            letterSpacing = 0.12.em,
            color = inkMuted,
        )
        Text(
            text = card.cardType.description(),
            fontWeight = FontWeight.Normal,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            color = ink,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.include_card_in_study_label),
                fontFamily = geistMono,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                letterSpacing = 0.04.em,
                color = ink,
                modifier = Modifier.weight(1f),
            )
            HToggle(checked = card.isActive, onCheckedChange = onActiveChanged)
        }

        if (card.explanation.isNotBlank()) {
            HAlert(
                title = stringResource(R.string.card_explanation_title),
                description = card.explanation,
                variant = AlertVariant.Default,
            )
        }

        EditablePreviewField(
            label = stringResource(R.string.card_front_label),
            value = card.prompt,
            placeholder = stringResource(R.string.card_front_placeholder),
            minLines = 2,
            errorMessage = validationIssues.cardMessage(card.cardId, PreviewCardField.Prompt, issueTextMapper),
            helperText = stringResource(R.string.card_front_supporting_text),
            supportingText = warningIssues.cardWarning(card.cardId, issueTextMapper),
            onValueChange = onPromptChanged,
        )
        EditablePreviewField(
            label = stringResource(R.string.card_answer_label),
            value = card.expectedAnswer,
            placeholder = stringResource(R.string.card_answer_placeholder),
            minLines = 2,
            errorMessage = validationIssues.cardMessage(card.cardId, PreviewCardField.ExpectedAnswer, issueTextMapper),
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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            ReviewBlockIconButton(
                icon = Icons.Outlined.Refresh,
                contentDescription = stringResource(R.string.regenerate_card_action),
                onClick = onRegenerate,
                enabled = regenerationTarget == null ||
                    regenerationTarget == PreviewRegenerationTarget.Card(card.cardId),
                accent = true,
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}
