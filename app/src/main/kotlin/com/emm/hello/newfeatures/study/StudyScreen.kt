package com.emm.hello.newfeatures.study

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emm.domain.flashcard.EvaluationMode
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FlashcardReview
import com.emm.domain.flashcard.GeneratedStudyCard
import com.emm.domain.flashcard.StudyCardType
import com.emm.domain.time.SystemClock
import com.emm.domain.study.ReviewGrade
import com.emm.hello.R
import com.emm.hello.core.audio.TextToSpeechManager
import com.emm.hello.core.audio.rememberTextToSpeechManager
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.core.ui.AlertVariant
import com.emm.hello.core.ui.BadgeVariant
import com.emm.hello.core.ui.ButtonVariant
import com.emm.hello.core.ui.HAlert
import com.emm.hello.core.ui.HAlertDialog
import com.emm.hello.core.ui.HBadge
import com.emm.hello.core.ui.HBadgeGroup
import com.emm.hello.core.ui.HButton
import com.emm.hello.core.ui.HInput
import com.emm.hello.core.ui.HProgressBar
import com.emm.hello.core.ui.HSeparator

private const val CARD_TRANSITION_DURATION_MS = 350
private const val CARD_TRANSITION_DIVISOR = 3
private const val CARD_EXIT_FADE_DURATION_MS = 250
private const val ANSWER_BUTTON_FADE_DURATION_MS = 200
private const val ANSWER_BUTTONS_PLACEHOLDER_HEIGHT_DP = 104
private const val PHONETIC_SEPARATOR_WIDTH_FRACTION = 0.4f
private const val MEANING_SEPARATOR_WIDTH_FRACTION = 0.5f
private const val SUPPORT_SEPARATOR_WIDTH_FRACTION = 0.7f
private const val MAX_RELATED_FORMS = 3

@Composable
fun StudyScreen(
    modifier: Modifier = Modifier,
    onBackRequested: () -> Unit = {},
    onFinishDialogDismissed: () -> Unit = {},
    onReviewAnswer: (StudySessionItem?, ReviewGrade) -> Unit = { _, _ -> },
    state: StudyUiState = StudyUiState(),
    showFinishDialog: Boolean = false,
) {
    val tts: TextToSpeechManager = rememberTextToSpeechManager()
    val isSpeaking by tts.isSpeaking.collectAsStateWithLifecycle()
    val ttsReady by tts.isReady.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current

    val prevStudyItem = remember { mutableStateOf(state.currentItem) }
    var cardFace by remember { mutableStateOf(CardFace.Front) }
    var typedAnswer by remember { mutableStateOf("") }
    var typedAnswerChecked by remember { mutableStateOf(false) }
    var typedAnswerCorrect by remember { mutableStateOf(false) }

    val progress = if (state.totalCount > 0) {
        state.reviewedCount.toFloat() / state.totalCount.toFloat()
    } else {
        0f
    }

    LaunchedEffect(state.currentItem?.studyCard?.cardId) {
        cardFace = CardFace.Front
        typedAnswer = ""
        typedAnswerChecked = false
        typedAnswerCorrect = false
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                stringResource(R.string.study_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            HBadge(
                                label = "${state.reviewedCount}/${state.totalCount}",
                                variant = BadgeVariant.Secondary,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackRequested) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.exit_session_desc),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            // ── Progress bar (shadcn-style) ─────────────────────────────────
            HProgressBar(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // ── Hint: "Toca para revelar" ───────────────────────────────
                Text(
                    text = if (cardFace == CardFace.Front) stringResource(R.string.tap_to_reveal) else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                )

                // ── Animated card with slide transition between flashcards ──
                AnimatedContent(
                    targetState = state.currentItem,
                    transitionSpec = {
                        (
                            slideInHorizontally(tween(CARD_TRANSITION_DURATION_MS)) {
                                it / CARD_TRANSITION_DIVISOR
                            } +
                                fadeIn(tween(CARD_TRANSITION_DURATION_MS))
                            )
                            .togetherWith(
                                slideOutHorizontally(tween(CARD_TRANSITION_DURATION_MS)) {
                                    -it / CARD_TRANSITION_DIVISOR
                                } + fadeOut(tween(CARD_EXIT_FADE_DURATION_MS))
                            )
                    },
                    label = "card_transition",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) { item ->
                    FlippableCard(
                        cardFace = cardFace,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            cardFace = it.next
                        },
                        progress = progress,
                        onFinished = { prevStudyItem.value = state.currentItem },
                        modifier = Modifier.fillMaxSize(),
                        frontContent = {
                            FlashcardFrontContent(
                                card = item?.flashcard,
                                studyCard = item?.studyCard,
                                prompt = item?.studyCard?.prompt ?: item?.flashcard?.word.orEmpty(),
                                phonetic = if (item?.studyCard?.sourceField == "word") {
                                    item.flashcard.phonetic
                                } else {
                                    ""
                                },
                                cardType = item?.studyCard?.cardType,
                            )
                        },
                        backContent = {
                            val currentItem = prevStudyItem.value
                            FlashcardBackContent(
                                card = currentItem?.flashcard,
                                studyCard = currentItem?.studyCard,
                                typedAnswer = typedAnswer,
                                typedAnswerChecked = typedAnswerChecked,
                                typedAnswerCorrect = typedAnswerCorrect,
                                isSpeaking = isSpeaking,
                                ttsReady = ttsReady,
                                onTypedAnswerChange = {
                                    typedAnswer = it
                                    typedAnswerChecked = false
                                },
                                onCheckTypedAnswer = {
                                    val activeCard = currentItem?.studyCard ?: return@FlashcardBackContent
                                    typedAnswerCorrect = matchesTypedAnswer(
                                        evaluationMode = activeCard.evaluationMode,
                                        typedAnswer = typedAnswer,
                                        expectedAnswer = activeCard.expectedAnswer,
                                        acceptedAnswers = activeCard.acceptedAnswers,
                                    )
                                    typedAnswerChecked = true
                                },
                                onStop = { tts.stop() },
                                onSpeak = {
                                    if (ttsReady) {
                                        tts.speak(state.currentItem?.flashcard?.word.orEmpty())
                                    }
                                },
                            )
                        },
                    )
                }

                // ── Answer buttons ──────────────────────────────────────────
                AnimatedContent(
                    targetState = cardFace == CardFace.Back,
                    transitionSpec = {
                        fadeIn(tween(ANSWER_BUTTON_FADE_DURATION_MS)) togetherWith
                            fadeOut(tween(ANSWER_BUTTON_FADE_DURATION_MS))
                    },
                    label = "answer_buttons",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                ) { showButtons ->
                    if (showButtons) {
                        val needsTypedAnswer = state.currentItem?.studyCard?.needsTypedAnswer == true
                        val gradePolicy = state.currentItem?.studyCard?.gradePolicy(
                            typedAnswerChecked = typedAnswerChecked,
                            typedAnswerCorrect = typedAnswerCorrect,
                        ) ?: ReviewGradePolicy()
                        if (!needsTypedAnswer || typedAnswerChecked) {
                            AnswerButtons(
                                enabledGrades = gradePolicy.enabledGrades,
                                guidance = gradePolicy.guidance,
                            ) { grade ->
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onReviewAnswer(state.currentItem, grade)
                            }
                        } else {
                            Spacer(Modifier.height(ANSWER_BUTTONS_PLACEHOLDER_HEIGHT_DP.dp))
                        }
                    } else {
                        Spacer(Modifier.height(ANSWER_BUTTONS_PLACEHOLDER_HEIGHT_DP.dp))
                    }
                }
            }
        }
    }

    if (showFinishDialog) {
        HAlertDialog(
            title = stringResource(R.string.session_completed_title),
            description = stringResource(R.string.session_completed_desc, state.totalCount),
            icon = Icons.Outlined.Check,
            confirmText = stringResource(R.string.back),
            cancelText = null,
            onConfirm = {
                onFinishDialogDismissed()
            },
            onDismiss = {
                onFinishDialogDismissed()
            },
        )
    }
}

// ── Front content ────────────────────────────────────────────────────────────

@Composable
private fun FlashcardFrontContent(
    card: Flashcard?,
    studyCard: GeneratedStudyCard?,
    prompt: String,
    phonetic: String,
    cardType: StudyCardType?,
) {
    val frontTitle = studyCard?.frontTitle().orEmpty()
    val frontSupport = studyCard?.frontSupportText(card).orEmpty()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp),
        ) {
            CardTypePromptBlock(
                card = card,
                studyCard = studyCard,
                prompt = prompt,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = prompt,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (frontTitle.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = frontTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            if (cardType != null) {
                Spacer(Modifier.height(12.dp))
                HBadge(
                    label = cardType.label,
                    variant = BadgeVariant.Outline,
                )
            }
            if (phonetic.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                HSeparator(modifier = Modifier.fillMaxWidth(PHONETIC_SEPARATOR_WIDTH_FRACTION))
                Spacer(Modifier.height(8.dp))
                Text(
                    text = phonetic,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            if (frontSupport.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                HSeparator(modifier = Modifier.fillMaxWidth(SUPPORT_SEPARATOR_WIDTH_FRACTION))
                Spacer(Modifier.height(12.dp))
                Text(
                    text = frontSupport,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ── Back content ─────────────────────────────────────────────────────────────

@Composable
private fun FlashcardBackContent(
    card: Flashcard?,
    studyCard: GeneratedStudyCard?,
    typedAnswer: String,
    typedAnswerChecked: Boolean,
    typedAnswerCorrect: Boolean,
    isSpeaking: Boolean,
    ttsReady: Boolean,
    onTypedAnswerChange: (String) -> Unit = {},
    onCheckTypedAnswer: () -> Unit = {},
    onStop: () -> Unit = {},
    onSpeak: () -> Unit = {},
) {
    val needsTypedAnswer = studyCard?.needsTypedAnswer == true
    val shouldRevealAnswer = !needsTypedAnswer || typedAnswerChecked
    val primaryText = studyCard?.expectedAnswer ?: card?.translation.orEmpty()
    val answerLabel = studyCard?.answerLabel() ?: "Respuesta"
    val inputLabel = studyCard?.typedAnswerLabel() ?: "Tu respuesta"
    val inputPlaceholder = studyCard?.typedAnswerPlaceholder(card) ?: "Escribe tu respuesta"
    val checkLabel = studyCard?.typedAnswerButtonLabel() ?: "Revisar"
    val resultMessage = studyCard?.typedAnswerResultMessage(typedAnswerCorrect).orEmpty()
    val supportingText = studyCard?.supportingBackText(card).orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (needsTypedAnswer && !typedAnswerChecked) {
            HInput(
                value = typedAnswer,
                onValueChange = onTypedAnswerChange,
                label = inputLabel,
                placeholder = inputPlaceholder,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onCheckTypedAnswer() }),
            )
            Spacer(Modifier.height(16.dp))
            HButton(
                text = checkLabel,
                onClick = onCheckTypedAnswer,
                variant = ButtonVariant.Default,
                enabled = typedAnswer.isNotBlank(),
            )
            Spacer(Modifier.height(20.dp))
        }

        if (shouldRevealAnswer) {
            Text(
                text = answerLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = primaryText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (needsTypedAnswer) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = resultMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (typedAnswerCorrect) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    textAlign = TextAlign.Center,
                )
            }

            if (supportingText.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                HSeparator(modifier = Modifier.fillMaxWidth(MEANING_SEPARATOR_WIDTH_FRACTION))
                Spacer(Modifier.height(12.dp))
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            studyCard?.let {
                CardTypeAnswerSupport(
                    card = card,
                    studyCard = it,
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── TTS button ──────────────────────────────────────────────────
        HButton(
            text = if (isSpeaking) stringResource(R.string.stop_speech_desc) else stringResource(R.string.speak_desc),
            onClick = { if (isSpeaking) onStop() else onSpeak() },
            enabled = ttsReady,
            variant = ButtonVariant.Ghost,
            leadingIcon = Icons.AutoMirrored.Filled.VolumeUp,
        )
    }
}

@Composable
private fun CardTypePromptBlock(
    card: Flashcard?,
    studyCard: GeneratedStudyCard?,
    prompt: String,
) {
    when (studyCard?.cardType) {
        StudyCardType.Cloze -> {
            HAlert(
                title = "Completa el hueco",
                description = prompt,
                variant = AlertVariant.Warning,
            )
        }

        StudyCardType.Form -> {
            val formHints = buildList {
                addAll(card?.irregularForms.orEmpty())
                if (card?.usagePattern?.isNotBlank() == true) {
                    add(card.usagePattern)
                }
            }.distinct()

            if (formHints.isNotEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Pistas de forma",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    HBadgeGroup {
                        formHints.take(2).forEach { hint ->
                            HBadge(
                                label = hint,
                                variant = BadgeVariant.Secondary,
                            )
                        }
                    }
                }
            }
        }

        else -> Unit
    }
}

@Composable
private fun CardTypeAnswerSupport(
    card: Flashcard?,
    studyCard: GeneratedStudyCard,
) {
    when (studyCard.cardType) {
        StudyCardType.Cloze -> {
            val context = card?.clozeSentence
                ?.takeIf(String::isNotBlank)
                ?: card?.sourceContext.orEmpty()
            if (context.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                HAlert(
                    title = "Contexto de apoyo",
                    description = context,
                    variant = AlertVariant.Default,
                )
            }
        }

        StudyCardType.Form -> {
            val forms = card?.irregularForms.orEmpty()
            if (forms.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Formas relacionadas",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    HBadgeGroup {
                        forms.take(MAX_RELATED_FORMS).forEach { form ->
                            HBadge(
                                label = form,
                                variant = BadgeVariant.Outline,
                            )
                        }
                    }
                }
            }
        }

        else -> Unit
    }
}

private fun GeneratedStudyCard.frontTitle(): String {
    return when (cardType) {
        StudyCardType.Recognition -> "Reconoce el significado o la idea principal"
        StudyCardType.Production -> "Recupera la expresion en ingles"
        StudyCardType.Cloze -> "Completa el hueco con la expresion correcta"
        StudyCardType.Form -> "Recupera la forma exacta"
    }
}

private fun GeneratedStudyCard.frontSupportText(card: Flashcard?): String {
    return when (cardType) {
        StudyCardType.Recognition -> recognitionSupportText(card)
        StudyCardType.Production -> productionSupportText(card)
        StudyCardType.Cloze -> clozeSupportText(card)
        StudyCardType.Form -> formSupportText(card)
    }
}

private fun GeneratedStudyCard.recognitionSupportText(card: Flashcard?): String {
    return when {
        hint.isNotBlank() -> hint
        explanation.isNotBlank() -> explanation
        card?.meaning?.isNotBlank() == true -> card.meaning
        else -> ""
    }
}

private fun GeneratedStudyCard.productionSupportText(card: Flashcard?): String {
    return when {
        hint.isNotBlank() -> hint
        card?.sourceContext?.isNotBlank() == true -> card.sourceContext
        card?.whyUseful?.isNotBlank() == true -> card.whyUseful
        else -> ""
    }
}

private fun GeneratedStudyCard.clozeSupportText(card: Flashcard?): String {
    return when {
        card?.sourceContext?.isNotBlank() == true -> card.sourceContext
        hint.isNotBlank() -> hint
        explanation.isNotBlank() -> explanation
        else -> ""
    }
}

private fun GeneratedStudyCard.formSupportText(card: Flashcard?): String {
    return when {
        card?.irregularForms?.isNotEmpty() == true -> {
            "Formas relacionadas: ${card.irregularForms.joinToString()}"
        }
        card?.usagePattern?.isNotBlank() == true -> card.usagePattern
        hint.isNotBlank() -> hint
        else -> ""
    }
}

private fun GeneratedStudyCard.answerLabel(): String {
    return when (cardType) {
        StudyCardType.Recognition -> "Significado esperado"
        StudyCardType.Production -> "Respuesta esperada"
        StudyCardType.Cloze -> "Expresion que completa la frase"
        StudyCardType.Form -> "Forma esperada"
    }
}

private fun GeneratedStudyCard.supportingBackText(card: Flashcard?): String {
    return when {
        explanation.isNotBlank() -> explanation
        hint.isNotBlank() -> hint
        cardType == StudyCardType.Cloze && card?.meaning?.isNotBlank() == true -> card.meaning
        cardType == StudyCardType.Form && card?.usagePattern?.isNotBlank() == true -> card.usagePattern
        card?.whyUseful?.isNotBlank() == true -> card.whyUseful
        card?.meaning?.isNotBlank() == true -> card.meaning
        else -> ""
    }
}

private fun GeneratedStudyCard.typedAnswerLabel(): String {
    return when (cardType) {
        StudyCardType.Recognition -> "Significado"
        StudyCardType.Production -> "Expresion en ingles"
        StudyCardType.Cloze -> "Completa la frase"
        StudyCardType.Form -> "Forma exacta"
    }
}

private fun GeneratedStudyCard.typedAnswerPlaceholder(card: Flashcard?): String {
    return when (cardType) {
        StudyCardType.Recognition -> "Escribe el significado esperado"
        StudyCardType.Production -> "Escribe la expresion en ingles"
        StudyCardType.Cloze -> "Escribe la palabra o frase faltante"
        StudyCardType.Form -> {
            if (card?.irregularForms?.isNotEmpty() == true) {
                "Escribe la forma pedida"
            } else {
                "Escribe la forma correcta"
            }
        }
    }
}

private fun GeneratedStudyCard.typedAnswerButtonLabel(): String {
    return when (evaluationMode) {
        EvaluationMode.Exact -> "Comprobar"
        EvaluationMode.FlexibleText -> "Comparar"
        EvaluationMode.ManualSelfCheck -> "Revisar"
    }
}

private fun GeneratedStudyCard.typedAnswerResultMessage(isCorrect: Boolean): String {
    if (isCorrect) {
        return when (evaluationMode) {
            EvaluationMode.Exact -> "Tu respuesta coincide exactamente con la esperada"
            EvaluationMode.FlexibleText -> "Tu respuesta coincide de forma aceptable"
            EvaluationMode.ManualSelfCheck -> ""
        }
    }

    return when (evaluationMode) {
        EvaluationMode.Exact -> "No coincide exactamente con la respuesta esperada"
        EvaluationMode.FlexibleText -> "No se parece lo suficiente a la respuesta esperada"
        EvaluationMode.ManualSelfCheck -> ""
    }
}

// ── Answer buttons with icons ────────────────────────────────────────────────

@Composable
private fun AnswerButtons(
    modifier: Modifier = Modifier,
    enabledGrades: Set<ReviewGrade> = ReviewGrade.entries.toSet(),
    guidance: String = "",
    onReviewAnswer: (ReviewGrade) -> Unit = {},
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (guidance.isNotBlank()) {
            Text(
                text = guidance,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HButton(
                text = stringResource(R.string.grade_again),
                onClick = { onReviewAnswer(ReviewGrade.AGAIN) },
                variant = ButtonVariant.Destructive,
                enabled = ReviewGrade.AGAIN in enabledGrades,
                modifier = Modifier.weight(1f),
                leadingIcon = Icons.Outlined.Refresh,
            )
            HButton(
                text = stringResource(R.string.grade_hard),
                onClick = { onReviewAnswer(ReviewGrade.HARD) },
                variant = ButtonVariant.Secondary,
                enabled = ReviewGrade.HARD in enabledGrades,
                modifier = Modifier.weight(1f),
                leadingIcon = Icons.Outlined.Warning,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HButton(
                text = stringResource(R.string.grade_good),
                onClick = { onReviewAnswer(ReviewGrade.GOOD) },
                variant = ButtonVariant.Default,
                enabled = ReviewGrade.GOOD in enabledGrades,
                modifier = Modifier.weight(1f),
                leadingIcon = Icons.Rounded.CheckCircle,
            )
            HButton(
                text = stringResource(R.string.grade_easy),
                onClick = { onReviewAnswer(ReviewGrade.EASY) },
                variant = ButtonVariant.Outline,
                enabled = ReviewGrade.EASY in enabledGrades,
                modifier = Modifier.weight(1f),
                leadingIcon = Icons.Rounded.Bolt,
            )
        }
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@PreviewLightDark
@Composable
private fun StudyScreenPreview() {
    HelloTheme {
        StudyScreen(
            state = StudyUiState(
                currentItem = StudySessionItem(
                    flashcard = Flashcard(
                        id = "1",
                        word = "Serendipity",
                        meaning = "The occurrence of events by chance in a happy way",
                        translation = "Casualidad afortunada",
                        examples = listOf(),
                        phonetic = "/ˌserənˈdɪpɪti/",
                        review = FlashcardReview.empty(SystemClock),
                    ),
                    studyCard = GeneratedStudyCard(
                        cardId = "study-card-1",
                        cardType = StudyCardType.Recognition,
                        prompt = "Serendipity",
                        expectedAnswer = "Casualidad afortunada",
                        evaluationMode = EvaluationMode.ManualSelfCheck,
                    ),
                ),
                reviewedCount = 3,
                totalCount = 10,
            ),
        )
    }
}

private val StudyCardType.label: String
    get() = when (this) {
        StudyCardType.Recognition -> "Recognition"
        StudyCardType.Production -> "Production"
        StudyCardType.Cloze -> "Cloze"
        StudyCardType.Form -> "Form"
    }
