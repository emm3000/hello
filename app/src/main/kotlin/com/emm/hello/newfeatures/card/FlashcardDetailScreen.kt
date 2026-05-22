package com.emm.hello.newfeatures.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.emm.domain.flashcard.Example
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardDetail
import com.emm.domain.flashcard.FlashcardReview
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.time.SystemClock
import com.emm.hello.R
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.theme.emberAccent
import com.emm.hello.core.theme.emberBg
import com.emm.hello.core.theme.emberDivider
import com.emm.hello.core.theme.emberFaint
import com.emm.hello.core.theme.emberHint
import com.emm.hello.core.theme.emberMuted
import com.emm.hello.core.theme.emberOnBg
import com.emm.hello.core.theme.emberPrimary
import com.emm.hello.core.theme.geist
import com.emm.hello.core.theme.geistMono
import com.emm.hello.core.theme.instrumentSerif
import com.emm.hello.core.ui.HAlertDialog
import com.emm.hello.core.ui.HChip
import com.emm.hello.core.ui.HDictSense
import com.emm.hello.core.ui.HDictSenseTone
import com.emm.hello.core.ui.HSectionLabel
import com.emm.hello.core.ui.HSeparator
import com.emm.hello.core.ui.HTopBar
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

private val screenHorizontalPadding = 20.dp

@Composable
fun FlashcardDetailScreen(
    modifier: Modifier = Modifier,
    flashcard: FlashcardDetail = FlashcardDetail(Flashcard.empty(SystemClock)),
    onNavigateBack: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onConfirmDelete: () -> Unit = {},
    onDismissDelete: () -> Unit = {},
    isDeleteConfirmationVisible: Boolean = false,
) {
    val card = flashcard.flashcard

    Surface(
        modifier = modifier.fillMaxSize(),
        color = emberBg,
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            FlashcardDetailTopBar(
                onBack = onNavigateBack,
                onEdit = onEditClick,
                onDelete = onDeleteClick,
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                FlashcardHero(card = card)

                val senses = remember(card) { dictSensesOf(card) }
                if (senses.isNotEmpty()) {
                    HeroDivider()
                    DictSensesColumn(senses = senses)
                }

                if (card.examples.isNotEmpty()) {
                    HeroDivider()
                    ExamplesBlock(examples = card.examples)
                }

                if (hasExtras(card)) {
                    HeroDivider()
                    ExtrasBlock(card = card)
                }

                if (hasContext(card)) {
                    HeroDivider()
                    ContextBlock(card = card)
                }

                HeroDivider()
                FooterBlock(detail = flashcard)
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (isDeleteConfirmationVisible) {
        HAlertDialog(
            title = stringResource(R.string.delete_flashcard_title),
            description = stringResource(R.string.delete_flashcard_description),
            icon = Icons.Outlined.Delete,
            confirmText = stringResource(R.string.delete),
            cancelText = stringResource(R.string.cancel),
            isDangerous = true,
            onConfirm = onConfirmDelete,
            onDismiss = onDismissDelete,
        )
    }
}

@Composable
private fun HeroDivider() {
    HSeparator(
        modifier = Modifier.padding(horizontal = screenHorizontalPadding, vertical = 24.dp),
        color = emberDivider,
    )
}

@Composable
private fun FlashcardDetailTopBar(
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    HTopBar(
        onBack = onBack,
        actions = {
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.edit),
                    tint = emberPrimary,
                )
            }
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.more_options),
                    tint = emberPrimary,
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.delete)) },
                    onClick = {
                        showMenu = false
                        onDelete()
                    },
                    leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                )
            }
        },
    )
}

@Composable
private fun FlashcardHero(card: Flashcard) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = screenHorizontalPadding, vertical = 16.dp),
    ) {
        HSectionLabel(label = stringResource(R.string.card_detail_header_label))
        Spacer(Modifier.height(12.dp))
        Text(
            text = card.word.ifBlank { "—" },
            fontFamily = instrumentSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 56.sp,
            lineHeight = 60.sp,
            letterSpacing = (-0.5).sp,
            color = emberOnBg,
        )

        val hasMetaRow = card.phonetic.isNotBlank() ||
            card.partOfSpeech.isNotBlank() ||
            card.levelBand.isNotBlank()

        if (hasMetaRow) {
            Spacer(Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (card.phonetic.isNotBlank()) {
                    Text(
                        text = card.phonetic,
                        fontFamily = geistMono,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        color = emberMuted,
                    )
                }
                if (card.partOfSpeech.isNotBlank()) {
                    Text(
                        text = card.partOfSpeech,
                        fontFamily = instrumentSerif,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Normal,
                        fontSize = 18.sp,
                        color = emberAccent,
                    )
                }
                if (card.levelBand.isNotBlank()) {
                    Text(
                        text = card.levelBand.uppercase(),
                        fontFamily = geistMono,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        letterSpacing = 0.12.em,
                        color = emberHint,
                    )
                }
            }
        }

        if (card.translation.isNotBlank()) {
            Spacer(Modifier.height(14.dp))
            Text(
                text = card.translation,
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

private data class DictSenseItem(
    val labelRes: Int,
    val body: String,
    val tone: HDictSenseTone,
)

private fun dictSensesOf(card: Flashcard): List<DictSenseItem> = buildList {
    if (card.meaning.isNotBlank()) {
        add(DictSenseItem(R.string.meaning_label, card.meaning, HDictSenseTone.Default))
    }
    if (card.usagePattern.isNotBlank()) {
        add(DictSenseItem(R.string.usage_pattern_label, card.usagePattern, HDictSenseTone.Default))
    }
    if (card.whyUseful.isNotBlank()) {
        add(DictSenseItem(R.string.why_useful_label, card.whyUseful, HDictSenseTone.Default))
    }
    if (card.commonMistake.isNotBlank()) {
        add(DictSenseItem(R.string.common_mistake_label, card.commonMistake, HDictSenseTone.Warn))
    }
    if (card.confusableWith.isNotEmpty()) {
        add(
            DictSenseItem(
                labelRes = R.string.confusable_with_label,
                body = card.confusableWith.joinToString(", "),
                tone = HDictSenseTone.Warn,
            ),
        )
    }
    if (card.noteSummary.isNotBlank()) {
        add(DictSenseItem(R.string.notes_label, card.noteSummary, HDictSenseTone.Default))
    }
}

@Composable
private fun DictSensesColumn(senses: List<DictSenseItem>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = screenHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        senses.forEachIndexed { index, sense ->
            HDictSense(
                index = index + 1,
                label = stringResource(sense.labelRes),
                body = sense.body,
                tone = sense.tone,
            )
        }
    }
}

@Composable
private fun ExamplesBlock(examples: List<Example>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = screenHorizontalPadding),
    ) {
        HSectionLabel(
            label = stringResource(R.string.card_detail_examples_section_label, examples.size),
        )
        Spacer(Modifier.height(16.dp))
        examples.forEachIndexed { index, example ->
            ExampleRow(example = example)
            if (index < examples.lastIndex) {
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun ExampleRow(example: Example) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = "·",
            fontFamily = instrumentSerif,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Normal,
            fontSize = 22.sp,
            color = emberAccent,
            modifier = Modifier.width(20.dp),
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            if (example.text.isNotBlank()) {
                Text(
                    text = example.text,
                    fontFamily = instrumentSerif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Normal,
                    fontSize = 17.sp,
                    lineHeight = 24.sp,
                    color = emberOnBg,
                )
            }
            if (example.translation.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = example.translation,
                    fontFamily = geist,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = emberMuted,
                )
            }
        }
    }
}

private fun hasExtras(card: Flashcard): Boolean =
    card.collocations.isNotEmpty() ||
        card.irregularForms.isNotEmpty() ||
        card.warnings.isNotEmpty() ||
        card.register.isNotBlank() ||
        card.learningDomain.isNotBlank() ||
        card.lemma.isNotBlank()

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExtrasBlock(card: Flashcard) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = screenHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        HSectionLabel(label = stringResource(R.string.card_detail_extras_section_label))

        if (card.collocations.isNotEmpty()) {
            ChipFlow(
                label = stringResource(R.string.collocations_label),
                values = card.collocations,
            )
        }
        if (card.irregularForms.isNotEmpty()) {
            ChipFlow(
                label = stringResource(R.string.irregular_forms_label),
                values = card.irregularForms,
            )
        }
        if (card.warnings.isNotEmpty()) {
            BulletList(
                label = stringResource(R.string.warnings_label),
                values = card.warnings,
            )
        }

        val metaPills = listOfNotNull(
            card.register.takeIf(String::isNotBlank)?.let {
                stringResource(R.string.register_label) to it
            },
            card.learningDomain.takeIf(String::isNotBlank)?.let {
                stringResource(R.string.domain_label) to it
            },
            card.lemma.takeIf(String::isNotBlank)?.let {
                stringResource(R.string.lemma_label) to it
            },
        )
        if (metaPills.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                metaPills.forEach { (label, value) ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = label.uppercase(),
                            fontFamily = geistMono,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.5.sp,
                            letterSpacing = 0.12.em,
                            color = emberMuted,
                            modifier = Modifier.width(96.dp),
                        )
                        Text(
                            text = value,
                            fontFamily = geist,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = emberOnBg,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipFlow(label: String, values: List<String>) {
    Column {
        Text(
            text = label.uppercase(),
            fontFamily = geistMono,
            fontWeight = FontWeight.Medium,
            fontSize = 10.5.sp,
            letterSpacing = 0.12.em,
            color = emberMuted,
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            values.forEach { value ->
                HChip(label = value, accent = false)
            }
        }
    }
}

@Composable
private fun BulletList(label: String, values: List<String>) {
    Column {
        Text(
            text = label.uppercase(),
            fontFamily = geistMono,
            fontWeight = FontWeight.Medium,
            fontSize = 10.5.sp,
            letterSpacing = 0.12.em,
            color = emberMuted,
        )
        Spacer(Modifier.height(6.dp))
        values.forEach { value ->
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = "·",
                    fontFamily = instrumentSerif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    color = emberAccent,
                    modifier = Modifier.width(16.dp),
                )
                Text(
                    text = value,
                    fontFamily = geist,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = emberOnBg,
                )
            }
        }
    }
}

private fun hasContext(card: Flashcard): Boolean =
    card.clozeSentence.isNotBlank() || card.sourceContext.isNotBlank()

@Composable
private fun ContextBlock(card: Flashcard) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = screenHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        HSectionLabel(label = stringResource(R.string.card_detail_context_section_label))
        if (card.clozeSentence.isNotBlank()) {
            Text(
                text = card.clozeSentence,
                fontFamily = instrumentSerif,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                color = emberOnBg,
            )
        }
        if (card.sourceContext.isNotBlank()) {
            Text(
                text = card.sourceContext,
                fontFamily = geist,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = emberMuted,
            )
        }
    }
}

@Composable
private fun FooterBlock(detail: FlashcardDetail) {
    val failedChecks = detail.qualityChecks.count { !it.passed }
    val studyCardsCount = detail.studyCards.size

    val parts = buildList {
        if (studyCardsCount > 0) {
            add(
                FooterPart(
                    text = footerStudyCards(studyCardsCount),
                    accent = false,
                ),
            )
        }
        if (failedChecks > 0) {
            add(
                FooterPart(
                    text = footerWarnings(failedChecks),
                    accent = true,
                ),
            )
        }
        add(FooterPart(text = footerNextReview(detail.flashcard.review), accent = false))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = screenHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        parts.forEachIndexed { index, part ->
            Text(
                text = part.text,
                fontFamily = geistMono,
                fontWeight = FontWeight.Normal,
                fontSize = 10.5.sp,
                letterSpacing = 0.12.em,
                color = if (part.accent) emberAccent else emberFaint,
            )
            if (index < parts.lastIndex) {
                Text(
                    text = " · ",
                    fontFamily = geistMono,
                    fontWeight = FontWeight.Normal,
                    fontSize = 10.5.sp,
                    color = emberFaint,
                )
            }
        }
    }
}

private data class FooterPart(val text: String, val accent: Boolean)

@Composable
private fun footerStudyCards(count: Int): String = if (count == 1) {
    stringResource(R.string.card_detail_footer_study_cards_one)
} else {
    stringResource(R.string.card_detail_footer_study_cards_other, count)
}

@Composable
private fun footerWarnings(count: Int): String = if (count == 1) {
    stringResource(R.string.card_detail_footer_warnings_one)
} else {
    stringResource(R.string.card_detail_footer_warnings_other, count)
}

@Composable
private fun footerNextReview(review: FlashcardReview): String {
    val today = LocalDate.now()
    val nextDate = Instant.ofEpochMilli(review.nextReviewAt)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    val days = ChronoUnit.DAYS.between(today, nextDate)
    return when {
        days < 0L -> stringResource(R.string.card_detail_footer_next_overdue)
        days == 0L -> stringResource(R.string.card_detail_footer_next_today)
        days == 1L -> stringResource(R.string.card_detail_footer_next_days_one)
        else -> stringResource(R.string.card_detail_footer_next_days_other, days.toInt())
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0E0C)
@Composable
private fun CardDetailScreenPreview() {
    HelloTheme {
        val sampleCard = FlashcardDetail(
            flashcard = Flashcard(
                id = "1".toFlashcardId(),
                word = "Aesthetic",
                phonetic = "/esˈTHedik/",
                translation = "Estético, visualmente atractivo.",
                partOfSpeech = "adjective",
                levelBand = "B2",
                meaning = "Concerned with beauty or the appreciation of beauty.",
                usagePattern = "Se usa para describir el atractivo visual de cosas o ambientes, " +
                    "más común en contextos de diseño que en el habla diaria.",
                whyUseful = "Permite hablar de gusto visual sin opinar sobre belleza intrínseca.",
                commonMistake = "No usar como sinónimo de “bonito” a secas; es más específico.",
                confusableWith = listOf("ascetic", "anaesthetic"),
                collocations = listOf("aesthetic appeal", "minimalist aesthetic", "purely aesthetic"),
                irregularForms = listOf("aesthetics (n.)", "aesthetically (adv.)"),
                warnings = listOf("Evitar en registros muy formales si hay un término técnico mejor."),
                register = "neutral",
                learningDomain = "design",
                lemma = "aesthetic",
                examples = listOf(
                    Example(
                        exampleId = "ex1",
                        text = "The new building has a very strong aesthetic.",
                        translation = "El nuevo edificio tiene una estética muy marcada.",
                        type = "",
                    ),
                    Example(
                        exampleId = "ex2",
                        text = "Her Instagram page is very aesthetic.",
                        translation = "Su perfil de Instagram es muy estético.",
                        type = "",
                    ),
                ),
                review = FlashcardReview.empty(SystemClock),
            ),
        )
        FlashcardDetailScreen(flashcard = sampleCard)
    }
}
