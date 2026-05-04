package com.emm.hello.newfeatures.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emm.domain.flashcard.GeneratedStudyCard
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
import com.emm.hello.newfeatures.card.validation.IssueTextMapper
import com.emm.hello.newfeatures.card.validation.PreviewCardField

@Composable
internal fun GeneratedStudyCardSummaryItem(
    index: Int,
    total: Int,
    card: GeneratedStudyCard,
    validationIssues: List<ValidationIssue>,
    warningIssues: List<ValidationIssue>,
    onClick: () -> Unit,
) {
    val issueTextMapper = IssueTextMapper()
    val issuesCount = listOfNotNull(
        validationIssues.cardMessage(card.cardId, PreviewCardField.Prompt, issueTextMapper),
        validationIssues.cardMessage(card.cardId, PreviewCardField.ExpectedAnswer, issueTextMapper),
        warningIssues.cardWarning(card.cardId, issueTextMapper),
    ).size

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
                Text(
                    text = card.explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = card.prompt,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val badgeLabel = if (issuesCount > 0) {
                    stringResource(R.string.card_issue_count, issuesCount)
                } else {
                    stringResource(R.string.card_ready_badge)
                }
                HBadge(
                    label = badgeLabel,
                    variant = if (issuesCount > 0) BadgeVariant.Destructive else BadgeVariant.Outline,
                )
                HButton(
                    text = stringResource(R.string.edit_card_action),
                    onClick = onClick,
                    variant = ButtonVariant.Ghost,
                    leadingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                )
            }
        }
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = card.cardType.displayName(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = card.cardType.description(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
        LabeledCheckbox(
            label = stringResource(R.string.include_card_in_study_label),
            checked = card.isActive,
            isEnabled = true,
            onCheckedChange = onActiveChanged,
        )
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
            HButton(
                text = stringResource(R.string.regenerate_card_action),
                onClick = onRegenerate,
                variant = ButtonVariant.Ghost,
                isLoading = regenerationTarget == PreviewRegenerationTarget.Card(card.cardId),
                enabled = regenerationTarget == null ||
                    regenerationTarget == PreviewRegenerationTarget.Card(card.cardId),
            )
        }
    }
}
