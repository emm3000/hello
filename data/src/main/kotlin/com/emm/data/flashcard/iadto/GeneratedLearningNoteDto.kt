package com.emm.data.flashcard.iadto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeneratedLearningNoteDto(
    @SerialName("note_id")
    val noteId: String,
    @SerialName("note_type")
    val noteType: String,
    val expression: String,
    @SerialName("intended_meaning_es")
    val intendedMeaningEs: String,
    @SerialName("simple_definition_en")
    val simpleDefinitionEn: String,
    @SerialName("part_of_speech")
    val partOfSpeech: String,
    val register: String,
    @SerialName("level_band")
    val levelBand: String,
    val domain: String,
    @SerialName("why_useful")
    val whyUseful: String,
    @SerialName("example_sentence")
    val exampleSentence: String,
    @SerialName("example_translation")
    val exampleTranslation: String,
    val cards: List<GeneratedStudyCardDto>,
    @SerialName("quality_checks")
    val qualityChecks: List<GeneratedNoteQualityCheckDto>,
    val lemma: String = "",
    val ipa: String = "",
    @SerialName("usage_pattern")
    val usagePattern: String = "",
    @SerialName("irregular_forms")
    val irregularForms: List<String> = emptyList(),
    val collocations: List<String> = emptyList(),
    @SerialName("common_mistake")
    val commonMistake: String = "",
    @SerialName("confusable_with")
    val confusableWith: List<String> = emptyList(),
    @SerialName("cloze_sentence")
    val clozeSentence: String = "",
    @SerialName("source_context")
    val sourceContext: String = "",
    val warnings: List<String> = emptyList(),
)

@Serializable
data class GeneratedStudyCardDto(
    @SerialName("card_id")
    val cardId: String,
    @SerialName("card_type")
    val cardType: String,
    val prompt: String,
    @SerialName("expected_answer")
    val expectedAnswer: String,
    @SerialName("evaluation_mode")
    val evaluationMode: String,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("accepted_answers")
    val acceptedAnswers: List<String> = emptyList(),
    val hint: String = "",
    val explanation: String = "",
    @SerialName("source_field")
    val sourceField: String = "",
)

@Serializable
data class GeneratedNoteQualityCheckDto(
    val code: String,
    val passed: Boolean,
    val message: String,
)
