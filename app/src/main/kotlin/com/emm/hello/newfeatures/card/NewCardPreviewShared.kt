package com.emm.hello.newfeatures.card

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.emm.domain.generation.EvaluationMode
import com.emm.domain.generation.GeneratedLearningNote
import com.emm.domain.generation.GeneratedNoteQualityCheck
import com.emm.domain.generation.GeneratedNoteQualityCode
import com.emm.domain.generation.LearningDomain
import com.emm.domain.generation.LearningNoteType
import com.emm.domain.generation.LevelBand
import com.emm.domain.generation.PartOfSpeechTag
import com.emm.domain.generation.RegisterPreference
import com.emm.domain.generation.StudyCardType
import com.emm.domain.validation.ValidationIssue
import com.emm.hello.R
import com.emm.hello.core.theme.instrumentAccent
import com.emm.hello.core.theme.instrumentDivider
import com.emm.hello.core.theme.instrumentMuted
import com.emm.hello.core.theme.instrumentOnBg
import com.emm.hello.core.theme.instrumentSurface
import com.emm.hello.core.theme.geistMono
import com.emm.hello.core.ui.AlertVariant
import com.emm.hello.core.ui.HAlert
import com.emm.hello.core.ui.HInput
import com.emm.hello.newfeatures.card.validation.IssueTextMapper
import com.emm.hello.newfeatures.card.validation.IssueUiTarget
import com.emm.hello.newfeatures.card.validation.PreviewCardField
import com.emm.hello.newfeatures.card.validation.PreviewField

private const val MAX_PREVIEW_COLLOCATIONS = 3

internal data class PreviewAlertModel(
    val title: String,
    val description: String,
    val variant: AlertVariant,
)

@Composable
internal fun PreviewAlertGroup(alerts: List<PreviewAlertModel>) {
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
internal fun EditablePreviewField(
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

/**
 * Square icon button (Ember-styled). 36dp square, surface bg, divider border,
 * with optional accent tint when [accent] is true.
 */
@Composable
internal fun ReviewBlockIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    accent: Boolean = false,
) {
    val tint = if (!enabled) instrumentMuted else if (accent) instrumentAccent else instrumentOnBg
    val border = if (accent) instrumentAccent else instrumentDivider
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(instrumentSurface, RoundedCornerShape(10.dp))
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
internal fun ReviewBlock(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onRegenerate: (() -> Unit)? = null,
    regenerateContentDescription: String = "",
    isRegenerating: Boolean = false,
    regenerateEnabled: Boolean = true,
    warn: Boolean = false,
    minLines: Int = 2,
    errorMessage: String? = null,
    supportingText: String? = null,
) {
    var editing by remember { mutableStateOf(false) }
    val displayPlaceholder = "—"

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label.uppercase(),
                fontFamily = geistMono,
                fontWeight = FontWeight.Medium,
                fontSize = 10.5.sp,
                letterSpacing = 0.12.em,
                color = if (warn) instrumentAccent else instrumentMuted,
                modifier = Modifier.weight(1f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReviewBlockIconButton(
                    icon = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.edit_card_action),
                    onClick = { editing = !editing },
                    accent = editing,
                )
                if (onRegenerate != null) {
                    ReviewBlockIconButton(
                        icon = Icons.Outlined.Refresh,
                        contentDescription = regenerateContentDescription,
                        onClick = onRegenerate,
                        enabled = regenerateEnabled && !isRegenerating,
                    )
                }
            }
        }

        if (editing) {
            HInput(
                value = value,
                onValueChange = onValueChange,
                placeholder = placeholder,
                singleLine = minLines == 1,
                minLines = minLines,
                maxLines = if (minLines == 1) 1 else 6,
                errorMessage = errorMessage,
                supportingText = mergeSupportingTexts(supportingText, null),
            )
        } else {
            val display = value.ifBlank { displayPlaceholder }
            Text(
                text = display,
                fontWeight = FontWeight.Normal,
                fontSize = 18.sp,
                lineHeight = 26.sp,
                color = when {
                    value.isBlank() -> instrumentMuted
                    warn -> instrumentAccent
                    else -> instrumentOnBg
                },
            )
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    fontFamily = geistMono,
                    fontSize = 11.sp,
                    letterSpacing = 0.04.em,
                    color = instrumentAccent,
                )
            } else if (supportingText != null) {
                Text(
                    text = supportingText,
                    fontFamily = geistMono,
                    fontSize = 11.sp,
                    letterSpacing = 0.04.em,
                    color = instrumentMuted,
                )
            }
        }
    }
}

@Composable
internal fun List<ValidationIssue>.noteFieldMessage(
    noteField: PreviewField,
    issueTextMapper: IssueTextMapper,
): String? {
    val issue = firstOrNull {
        issueTextMapper.map(it).target == IssueUiTarget.PreviewFieldTarget(noteField)
    } ?: return null
    return stringResource(issueTextMapper.map(issue).textResId)
}

@Composable
internal fun List<ValidationIssue>.cardMessage(
    cardId: String,
    field: PreviewCardField,
    issueTextMapper: IssueTextMapper,
): String? {
    val issue = firstOrNull {
        issueTextMapper.map(it).target == IssueUiTarget.PreviewCard(cardId = cardId, field = field)
    } ?: return null
    return stringResource(issueTextMapper.map(issue).textResId)
}

@Composable
internal fun List<ValidationIssue>.cardWarning(cardId: String, issueTextMapper: IssueTextMapper): String? {
    val issue = firstOrNull {
        issueTextMapper.map(it).target == IssueUiTarget.PreviewCard(
            cardId = cardId,
            field = PreviewCardField.Active,
        )
    } ?: return null
    return stringResource(issueTextMapper.map(issue).textResId)
}

internal fun GeneratedLearningNote.meaningAlerts(): List<PreviewAlertModel> {
    return qualityChecks.failedAlertsFor(
        GeneratedNoteQualityCode.SingleMeaning,
        GeneratedNoteQualityCode.RequiredFieldsPresent,
    )
}

internal fun GeneratedLearningNote.exampleAlerts(): List<PreviewAlertModel> {
    return qualityChecks.failedAlertsFor(
        GeneratedNoteQualityCode.NaturalExample,
        GeneratedNoteQualityCode.ExampleSupportsMeaning,
    )
}

internal fun GeneratedLearningNote.cardSectionAlerts(): List<PreviewAlertModel> {
    return qualityChecks.failedAlertsFor(
        GeneratedNoteQualityCode.ClearCardFocus,
        GeneratedNoteQualityCode.NonAmbiguousAnswers,
        GeneratedNoteQualityCode.NoteCardAlignment,
    )
}

internal fun GeneratedLearningNote.collocationsPreview(): List<String> {
    return collocations.take(MAX_PREVIEW_COLLOCATIONS)
}

internal fun List<GeneratedNoteQualityCheck>.failedAlertsFor(
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

internal fun GeneratedNoteQualityCode.toAlertTitle(): String {
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

internal fun LearningNoteType.displayName(): String {
    return when (this) {
        LearningNoteType.Word -> "Palabra"
        LearningNoteType.Phrase -> "Frase"
        LearningNoteType.PhrasalVerb -> "Phrasal verb"
        LearningNoteType.Idiom -> "Idiom"
        LearningNoteType.SentencePattern -> "Patrón"
    }
}

internal fun PartOfSpeechTag.displayName(): String {
    return when (this) {
        PartOfSpeechTag.Noun -> "noun"
        PartOfSpeechTag.Verb -> "verb"
        PartOfSpeechTag.Adjective -> "adjective"
        PartOfSpeechTag.Adverb -> "adverb"
        PartOfSpeechTag.Preposition -> "preposition"
        PartOfSpeechTag.Conjunction -> "conjunction"
        PartOfSpeechTag.Interjection -> "interjection"
        PartOfSpeechTag.PhrasalVerb -> "phrasal verb"
        PartOfSpeechTag.Idiom -> "idiom"
        PartOfSpeechTag.Chunk -> "chunk"
        PartOfSpeechTag.Other -> "otro"
    }
}

internal fun RegisterPreference.displayName(): String {
    return when (this) {
        RegisterPreference.Casual -> "Casual"
        RegisterPreference.Neutral -> "Neutral"
        RegisterPreference.Formal -> "Formal"
    }
}

internal fun LevelBand.displayName(): String {
    return when (this) {
        LevelBand.A1_A2 -> "A1-A2"
        LevelBand.B1_B2 -> "B1-B2"
        LevelBand.C1_PLUS -> "C1+"
    }
}

internal fun LearningDomain.displayName(): String {
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

internal fun StudyCardType.displayName(): String {
    return when (this) {
        StudyCardType.Recognition -> "Reconocimiento"
        StudyCardType.Production -> "Producción"
        StudyCardType.Cloze -> "Cloze"
        StudyCardType.Form -> "Forma"
    }
}

internal fun StudyCardType.description(): String {
    return when (this) {
        StudyCardType.Recognition -> "Sirve para reconocer el significado o uso correcto."
        StudyCardType.Production -> "Sirve para producir la expresión por tu cuenta."
        StudyCardType.Cloze -> "Sirve para completar una frase con la forma correcta."
        StudyCardType.Form -> "Sirve para fijarte en la forma exacta de la expresión."
    }
}

internal fun EvaluationMode.displayName(): String {
    return when (this) {
        EvaluationMode.Exact -> "Respuesta exacta"
        EvaluationMode.FlexibleText -> "Texto flexible"
        EvaluationMode.ManualSelfCheck -> "Autoevaluación"
    }
}

internal fun String.sourceFieldDisplayName(): String {
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

private fun mergeSupportingTexts(vararg values: String?): String? {
    val lines = values.filterNot { it.isNullOrBlank() }
    return if (lines.isEmpty()) null else lines.joinToString(separator = "\n")
}
