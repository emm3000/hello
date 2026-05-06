package com.emm.hello.newfeatures.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.emm.hello.core.ui.AlertVariant
import com.emm.hello.core.ui.BadgeVariant
import com.emm.hello.core.ui.ButtonVariant
import com.emm.hello.core.ui.HAlert
import com.emm.hello.core.ui.HBadge
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HInput
import com.emm.hello.core.ui.HSkeleton
import com.emm.hello.newfeatures.card.validation.IssueTextMapper
import com.emm.hello.newfeatures.card.validation.IssueUiTarget
import com.emm.hello.newfeatures.card.validation.PreviewCardField
import com.emm.hello.newfeatures.card.validation.PreviewField

private const val MAX_PREVIEW_COLLOCATIONS = 3
private const val SKELETON_DETAIL_WIDTH = 0.4f

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

@Composable
internal fun RegenerateFieldButton(
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
            enabled = noteRegenerationTarget == null ||
                noteRegenerationTarget == PreviewRegenerationTarget.Field(field),
        )
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

internal fun GeneratedLearningNote.noteSectionSummary(): String {
    val meaning = simpleDefinitionEn.value.ifBlank { intendedMeaningEs.value }
    val useful = whyUseful.ifBlank { "Sin explicación adicional todavía." }
    return "$meaning\n$useful"
}

internal fun GeneratedLearningNote.exampleSectionSummary(): String {
    val sentence = exampleSentence.ifBlank { "Sin ejemplo principal." }
    val translation = exampleTranslation.ifBlank { "Sin traducción del ejemplo." }
    return "$sentence\n$translation"
}

internal fun GeneratedLearningNote.cardsSectionSummary(): String {
    val activeCount = cards.count { it.isActive }
    val firstCard = cards.firstOrNull()
    return if (firstCard == null) {
        "No hay tarjetas derivadas."
    } else {
        "$activeCount de ${cards.size} activas. Primera tarjeta: ${firstCard.cardType.displayName()}."
    }
}

@Composable
internal fun PreviewOverview(note: GeneratedLearningNote) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = note.expression.value,
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
internal fun PreviewSectionHeader(
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
internal fun LoadingStepSkeleton(
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
                    .height(14.dp),
            )
        }
    }
}

@Composable
internal fun InfoRow(label: String, value: String) {
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
