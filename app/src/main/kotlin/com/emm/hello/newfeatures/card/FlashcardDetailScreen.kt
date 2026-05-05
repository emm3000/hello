
package com.emm.hello.newfeatures.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.domain.flashcard.Example
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardReview
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.time.SystemClock
import com.emm.hello.R
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.ui.BadgeVariant
import com.emm.hello.core.ui.ButtonVariant
import com.emm.hello.core.ui.CardVariant
import com.emm.hello.core.ui.HBadge
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HCard
import com.emm.hello.core.ui.HSeparator

@Composable
fun FlashcardDetailScreen(
    modifier: Modifier = Modifier,
    flashcard: Flashcard = Flashcard.empty(SystemClock),
    onNavigateBack: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        flashcard.word,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Main card ─────────────────────────────────────────────
            HCard(
                modifier = Modifier.fillMaxWidth(),
                variant = CardVariant.Elevated,
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = flashcard.word,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (flashcard.partOfSpeech.isNotBlank()) {
                            HBadge(
                                label = flashcard.partOfSpeech,
                                variant = BadgeVariant.Secondary,
                            )
                        }
                    }
                    Text(
                        text = flashcard.phonetic,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(Modifier.height(8.dp))
                    HSeparator()
                    Spacer(Modifier.height(12.dp))

                    DetailItem(label = stringResource(R.string.translation_label), value = flashcard.translation)
                    Spacer(Modifier.height(12.dp))
                    DetailItem(label = stringResource(R.string.meaning_label), value = flashcard.meaning)
                    FlashcardRichSections(flashcard)
                }
            }
        }
    }
}

@Composable
private fun FlashcardRichSections(flashcard: Flashcard) {
    OptionalDetailSection(
        label = stringResource(R.string.notes_label),
        value = flashcard.noteSummary,
    )
    OptionalDetailSection(
        label = stringResource(R.string.why_useful_label),
        value = flashcard.whyUseful,
    )
    OptionalDetailSection(
        label = stringResource(R.string.usage_pattern_label),
        value = flashcard.usagePattern,
    )
    OptionalDetailSection(
        label = stringResource(R.string.common_mistake_label),
        value = flashcard.commonMistake,
    )
    LearningMetadataSection(flashcard)
    RichListSection(
        title = stringResource(R.string.collocations_label),
        values = flashcard.collocations,
    )
    RichListSection(
        title = stringResource(R.string.irregular_forms_label),
        values = flashcard.irregularForms,
    )
    RichListSection(
        title = stringResource(R.string.confusable_with_label),
        values = flashcard.confusableWith,
    )
    RichListSection(
        title = stringResource(R.string.warnings_label),
        values = flashcard.warnings,
    )
    OptionalDetailSection(
        label = stringResource(R.string.cloze_sentence_label),
        value = flashcard.clozeSentence,
    )
    OptionalDetailSection(
        label = stringResource(R.string.source_context_label),
        value = flashcard.sourceContext,
    )
    ExamplesSection(flashcard.examples)
    StudyCardsSection(flashcard)
    QualityChecksSection(flashcard)
}

@Composable
private fun OptionalDetailSection(label: String, value: String) {
    if (value.isBlank()) return

    Spacer(Modifier.height(4.dp))
    HSeparator()
    Spacer(Modifier.height(12.dp))
    DetailItem(label = label, value = value)
}

@Composable
private fun LearningMetadataSection(flashcard: Flashcard) {
    val metadataItems = listOfNotNull(
        flashcard.register.takeIf(String::isNotBlank)?.let {
            stringResource(R.string.register_label) to it
        },
        flashcard.levelBand.takeIf(String::isNotBlank)?.let {
            stringResource(R.string.level_label) to it
        },
        flashcard.learningDomain.takeIf(String::isNotBlank)?.let {
            stringResource(R.string.domain_label) to it
        },
        flashcard.lemma.takeIf(String::isNotBlank)?.let {
            stringResource(R.string.lemma_label) to it
        },
    )
    if (metadataItems.isEmpty()) return

    Spacer(Modifier.height(4.dp))
    HSeparator()
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.learning_metadata_label),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.height(8.dp))
    metadataItems.forEach { (label, value) ->
        DetailItem(label = label, value = value)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ExamplesSection(examples: List<Example>) {
    if (examples.isEmpty()) return

    Spacer(Modifier.height(4.dp))
    HSeparator()
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.examples_label),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.height(8.dp))

    examples.forEachIndexed { index, example ->
        key(example.exampleId) {
            ExampleItem(index = index + 1, example = example)
            if (index < examples.lastIndex) {
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun StudyCardsSection(flashcard: Flashcard) {
    if (flashcard.studyCards.isEmpty()) return

    Spacer(Modifier.height(4.dp))
    HSeparator()
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.generated_cards_label),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.height(8.dp))
    flashcard.studyCards.forEachIndexed { index, card ->
        key(card.cardId) {
            StudyCardItem(index = index + 1, card = card)
            if (index < flashcard.studyCards.lastIndex) {
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun QualityChecksSection(flashcard: Flashcard) {
    if (flashcard.qualityChecks.isEmpty()) return

    Spacer(Modifier.height(4.dp))
    HSeparator()
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.quality_checks_label),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.height(8.dp))
    flashcard.qualityChecks.forEach { check ->
        DetailItem(
            label = check.code.name,
            value = if (check.passed) {
                stringResource(R.string.quality_check_passed, check.message)
            } else {
                stringResource(R.string.quality_check_failed, check.message)
            }
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun RichListSection(title: String, values: List<String>) {
    if (values.isEmpty()) return

    Spacer(Modifier.height(4.dp))
    HSeparator()
    Spacer(Modifier.height(12.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.height(8.dp))
    values.forEach { value ->
        Text(
            text = "• $value",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun StudyCardItem(index: Int, card: com.emm.domain.flashcard.GeneratedStudyCard) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "$index. ${card.cardType.name}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        DetailItem(label = stringResource(R.string.prompt_label), value = card.prompt)
        DetailItem(label = stringResource(R.string.expected_answer_label), value = card.expectedAnswer)
        if (card.hint.isNotBlank()) {
            DetailItem(label = stringResource(R.string.hint_label), value = card.hint)
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
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
    var showTranslation by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "$index. ${example.text}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (showTranslation) {
            Text(
                text = example.translation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val buttonText = if (showTranslation) {
                stringResource(R.string.hide_translation)
            } else {
                stringResource(R.string.show_translation)
            }
            HButton(
                text = buttonText,
                onClick = { showTranslation = !showTranslation },
                variant = ButtonVariant.Ghost,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CardDetailScreenPreview() {
    HelloTheme {
        val sampleCard = Flashcard(
            id = "1".toFlashcardId(),
            word = "Aesthetic",
            phonetic = "/esˈTHedik/",
            translation = "Estético",
            meaning = "Concerned with beauty or the appreciation of beauty.",
            examples = listOf(
                Example(
                    exampleId = "ex1",
                    text = "The new building has a very aesthetic design.",
                    translation = "El nuevo edificio tiene un diseño muy estético.",
                    type = "",
                ),
                Example(
                    exampleId = "ex2",
                    text = "Her Instagram page is very aesthetic.",
                    translation = "Su página de Instagram es muy estética.",
                    type = "",
                ),
            ),
            review = FlashcardReview.empty(SystemClock),
        )
        FlashcardDetailScreen(flashcard = sampleCard)
    }
}
