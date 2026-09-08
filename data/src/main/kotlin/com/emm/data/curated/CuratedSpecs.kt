package com.emm.data.curated

import com.emm.domain.flashcard.toDefinitionEn
import com.emm.domain.flashcard.toExpression
import com.emm.domain.flashcard.toIntendedMeaningEs
import com.emm.domain.generation.EvaluationMode
import com.emm.domain.generation.GeneratedLearningNote
import com.emm.domain.generation.GeneratedNoteQualityCheck
import com.emm.domain.generation.GeneratedNoteQualityCode
import com.emm.domain.generation.GeneratedStudyCard
import com.emm.domain.generation.LearningDomain
import com.emm.domain.generation.LearningNoteType
import com.emm.domain.generation.LevelBand
import com.emm.domain.generation.PartOfSpeechTag
import com.emm.domain.generation.RegisterPreference
import com.emm.domain.generation.StudyCardType
import com.emm.domain.text.lowercaseRoot

internal data class WordSpec(
    val expression: String,
    val partOfSpeech: PartOfSpeechTag,
    val levelBand: LevelBand,
    val domain: LearningDomain,
    val ipa: String,
    val definitionEn: String,
    val meaningEs: String,
    val whyUseful: String,
    val example: String,
    val translation: String,
    val commonMistake: String,
    val confusableWith: List<String>,
    val falseFriend: String = "",
    val sourceContext: String = "",
    val register: RegisterPreference = RegisterPreference.Neutral,
    val collocations: List<String> = emptyList(),
    val acceptedAnswers: List<String> = emptyList(),
    val productionHint: String = "",
)

internal data class CollocationSpec(
    val expression: String,
    val levelBand: LevelBand,
    val domain: LearningDomain,
    val ipa: String,
    val definitionEn: String,
    val meaningEs: String,
    val whyUseful: String,
    val example: String,
    val translation: String,
    val commonMistake: String,
    val confusableWith: List<String>,
    val collocations: List<String>,
    val usagePattern: String,
    val sourceContext: String,
    val clozeAnswer: String,
    val register: RegisterPreference = RegisterPreference.Neutral,
    val acceptedAnswers: List<String> = emptyList(),
    val productionHint: String = "",
)

internal data class SentencePatternSpec(
    val expression: String,
    val levelBand: LevelBand,
    val domain: LearningDomain,
    val definitionEn: String,
    val meaningEs: String,
    val whyUseful: String,
    val example: String,
    val translation: String,
    val commonMistake: String,
    val confusableWith: List<String>,
    val collocations: List<String>,
    val usagePattern: String,
    val clozeSentence: String,
    val clozeAnswer: String,
    val sourceContext: String,
    val register: RegisterPreference = RegisterPreference.Neutral,
    val acceptedAnswers: List<String> = emptyList(),
)

internal fun WordSpec.toNote(deckId: String): GeneratedLearningNote {
    val noteId: String = curatedNoteId(deckId = deckId, expression = expression)
    return GeneratedLearningNote(
        noteId = noteId,
        noteType = LearningNoteType.Word,
        expression = expression.toExpression(),
        intendedMeaningEs = meaningEs.toIntendedMeaningEs(),
        simpleDefinitionEn = definitionEn.toDefinitionEn(),
        partOfSpeech = partOfSpeech,
        register = register,
        levelBand = levelBand,
        domain = domain,
        whyUseful = whyUseful,
        exampleSentence = example,
        exampleTranslation = translation,
        cards = listOf(
            recognitionCard(
                noteId = noteId,
                prompt = wordRecognitionPrompt(expression = expression, falseFriend = falseFriend),
                meaningEs = meaningEs,
            ),
            productionCard(
                noteId = noteId,
                meaningEs = meaningEs,
                expression = expression,
                hint = productionHint,
                acceptedAnswers = acceptedAnswers,
            ),
        ),
        qualityChecks = curatedQualityChecks,
        lemma = expression,
        ipa = ipa,
        collocations = collocations,
        commonMistake = commonMistake,
        confusableWith = confusableWith,
        sourceContext = wordSourceContext(
            sourceContext = sourceContext,
            expression = expression,
            falseFriend = falseFriend,
        ),
    )
}

internal fun CollocationSpec.toNote(deckId: String): GeneratedLearningNote {
    val noteId: String = curatedNoteId(deckId = deckId, expression = expression)
    return GeneratedLearningNote(
        noteId = noteId,
        noteType = LearningNoteType.Phrase,
        expression = expression.toExpression(),
        intendedMeaningEs = meaningEs.toIntendedMeaningEs(),
        simpleDefinitionEn = definitionEn.toDefinitionEn(),
        partOfSpeech = PartOfSpeechTag.Chunk,
        register = register,
        levelBand = levelBand,
        domain = domain,
        whyUseful = whyUseful,
        exampleSentence = example,
        exampleTranslation = translation,
        cards = listOf(
            recognitionCard(
                noteId = noteId,
                prompt = "What does '$expression' mean?",
                meaningEs = meaningEs,
            ),
            productionCard(
                noteId = noteId,
                meaningEs = meaningEs,
                expression = expression,
                hint = productionHint,
                acceptedAnswers = acceptedAnswers,
            ),
            clozeCard(
                noteId = noteId,
                prompt = example.replace(clozeAnswer, CLOZE_BLANK),
                answer = clozeAnswer,
                sourceField = "example_sentence",
            ),
        ),
        qualityChecks = curatedQualityChecks,
        ipa = ipa,
        usagePattern = usagePattern,
        collocations = collocations,
        commonMistake = commonMistake,
        confusableWith = confusableWith,
        sourceContext = sourceContext,
    )
}

internal fun SentencePatternSpec.toNote(deckId: String): GeneratedLearningNote {
    val noteId: String = curatedNoteId(deckId = deckId, expression = expression)
    return GeneratedLearningNote(
        noteId = noteId,
        noteType = LearningNoteType.SentencePattern,
        expression = expression.toExpression(),
        intendedMeaningEs = meaningEs.toIntendedMeaningEs(),
        simpleDefinitionEn = definitionEn.toDefinitionEn(),
        partOfSpeech = PartOfSpeechTag.Chunk,
        register = register,
        levelBand = levelBand,
        domain = domain,
        whyUseful = whyUseful,
        exampleSentence = example,
        exampleTranslation = translation,
        cards = listOf(
            recognitionCard(
                noteId = noteId,
                prompt = "What does '$expression' mean?",
                meaningEs = meaningEs,
            ),
            productionCard(
                noteId = noteId,
                meaningEs = meaningEs,
                expression = expression,
                hint = "",
                acceptedAnswers = acceptedAnswers,
            ),
            clozeCard(
                noteId = noteId,
                prompt = clozeSentence,
                answer = clozeAnswer,
                sourceField = "cloze_sentence",
            ),
        ),
        qualityChecks = curatedQualityChecks,
        usagePattern = usagePattern,
        collocations = collocations,
        commonMistake = commonMistake,
        confusableWith = confusableWith,
        clozeSentence = clozeSentence,
        sourceContext = sourceContext,
    )
}

private fun curatedNoteId(deckId: String, expression: String): String =
    "curated.$deckId.${expression.toCatalogSlug()}"

private fun String.toCatalogSlug(): String =
    lowercaseRoot().replace(' ', '-').replace(charactersOutsideCatalogSlug, "")

private fun wordRecognitionPrompt(expression: String, falseFriend: String): String {
    val question = "What does '$expression' mean?"
    return if (falseFriend.isBlank()) question else "$question (careful: not '$falseFriend')"
}

private fun wordSourceContext(
    sourceContext: String,
    expression: String,
    falseFriend: String,
): String = when {
    sourceContext.isNotBlank() -> sourceContext
    falseFriend.isBlank() -> ""
    else -> "Falso amigo: $expression ≠ $falseFriend"
}

private fun recognitionCard(
    noteId: String,
    prompt: String,
    meaningEs: String,
): GeneratedStudyCard = GeneratedStudyCard(
    cardId = "$noteId.recognition",
    cardType = StudyCardType.Recognition,
    prompt = prompt,
    expectedAnswer = meaningEs,
    evaluationMode = EvaluationMode.FlexibleText,
    sourceField = "expression",
)

private fun productionCard(
    noteId: String,
    meaningEs: String,
    expression: String,
    hint: String,
    acceptedAnswers: List<String>,
): GeneratedStudyCard = GeneratedStudyCard(
    cardId = "$noteId.production",
    cardType = StudyCardType.Production,
    prompt = productionPrompt(meaningEs = meaningEs, hint = hint),
    expectedAnswer = expression,
    evaluationMode = EvaluationMode.Exact,
    acceptedAnswers = acceptedAnswers,
    sourceField = "expression",
)

private fun productionPrompt(meaningEs: String, hint: String): String =
    if (hint.isBlank()) "English for '$meaningEs'" else "English for '$meaningEs' ($hint)"

private fun clozeCard(
    noteId: String,
    prompt: String,
    answer: String,
    sourceField: String,
): GeneratedStudyCard = GeneratedStudyCard(
    cardId = "$noteId.cloze",
    cardType = StudyCardType.Cloze,
    prompt = prompt,
    expectedAnswer = answer,
    evaluationMode = EvaluationMode.Exact,
    sourceField = sourceField,
)

private const val CLOZE_BLANK = "___"

private val charactersOutsideCatalogSlug: Regex = Regex("[^a-z0-9-]")

private val curatedQualityChecks: List<GeneratedNoteQualityCheck> = listOf(
    GeneratedNoteQualityCheck(
        code = GeneratedNoteQualityCode.SingleMeaning,
        passed = true,
        message = "One meaning, one target expression.",
    ),
    GeneratedNoteQualityCheck(
        code = GeneratedNoteQualityCode.NaturalExample,
        passed = true,
        message = "The example reads as natural English.",
    ),
    GeneratedNoteQualityCheck(
        code = GeneratedNoteQualityCode.NonAmbiguousAnswers,
        passed = true,
        message = "Expected answers are short and objective.",
    ),
    GeneratedNoteQualityCheck(
        code = GeneratedNoteQualityCode.ClearCardFocus,
        passed = true,
        message = "Each card tests one thing.",
    ),
    GeneratedNoteQualityCheck(
        code = GeneratedNoteQualityCode.NoteCardAlignment,
        passed = true,
        message = "All cards target the same expression and meaning.",
    ),
)
