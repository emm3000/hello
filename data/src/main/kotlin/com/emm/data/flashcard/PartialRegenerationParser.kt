package com.emm.data.flashcard

import com.emm.domain.flashcard.EvaluationMode
import com.emm.domain.flashcard.GeneratedExampleDraft
import com.emm.domain.flashcard.GeneratedStudyCard
import com.emm.domain.flashcard.StudyCardType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object PartialRegenerationParser {

    fun parseField(raw: String, json: Json, jsonKey: String, label: String): String {
        val response = decode<FieldRegenerationResponseDto>(raw, json, label)
        val data = response.data ?: throw invalidPayload(label)
        val value = when (jsonKey) {
            "why_useful" -> data.whyUseful
            "usage_pattern" -> data.usagePattern
            "common_mistake" -> data.commonMistake
            else -> null
        }
        require(!value.isNullOrBlank()) { "La IA devolvio un valor vacio para $jsonKey." }
        return value
    }

    fun parseExample(raw: String, json: Json): GeneratedExampleDraft {
        val response = decode<ExampleRegenerationResponseDto>(raw, json, "ejemplo")
        val data = response.data ?: throw invalidPayload("ejemplo")
        require(data.exampleSentence.isNotBlank()) { "La IA devolvio un example_sentence vacio." }
        require(data.exampleTranslation.isNotBlank()) { "La IA devolvio un example_translation vacio." }
        return GeneratedExampleDraft(
            sentence = data.exampleSentence,
            translation = data.exampleTranslation,
        )
    }

    fun parseCloze(raw: String, json: Json): String {
        val response = decode<ClozeRegenerationResponseDto>(raw, json, "cloze")
        val data = response.data ?: throw invalidPayload("cloze")
        require(data.clozeSentence.isNotBlank()) { "La IA devolvio un cloze_sentence vacio." }
        return data.clozeSentence
    }

    fun parseStudyCard(raw: String, json: Json): GeneratedStudyCard {
        val response = decode<StudyCardRegenerationResponseDto>(raw, json, "study card")
        val data = response.data ?: throw invalidPayload("study card")
        val card = data.card ?: throw invalidPayload("study card")
        require(card.cardId.isNotBlank()) { "La IA devolvio un card_id vacio." }
        require(card.prompt.isNotBlank()) { "La IA devolvio un prompt vacio." }
        require(card.expectedAnswer.isNotBlank()) { "La IA devolvio un expected_answer vacio." }
        return GeneratedStudyCard(
            cardId = card.cardId,
            cardType = card.cardType.toStudyCardType(),
            prompt = card.prompt,
            expectedAnswer = card.expectedAnswer,
            evaluationMode = card.evaluationMode.toEvaluationMode(),
            isActive = card.isActive,
            acceptedAnswers = card.acceptedAnswers,
            hint = card.hint,
            explanation = card.explanation,
            sourceField = card.sourceField,
        )
    }

    private inline fun <reified T> decode(raw: String, json: Json, label: String): T {
        return runCatching { json.decodeFromString<T>(raw) }
            .getOrElse {
                throw IllegalArgumentException(
                    "La respuesta de la IA no coincide con el formato esperado para $label",
                    it,
                )
            }
    }

    private fun invalidPayload(label: String): IllegalArgumentException {
        return IllegalArgumentException("La respuesta de la IA no contiene data valida para $label.")
    }

    private fun String.toStudyCardType(): StudyCardType {
        return when (lowercase()) {
            "recognition" -> StudyCardType.Recognition
            "production" -> StudyCardType.Production
            "cloze" -> StudyCardType.Cloze
            "form" -> StudyCardType.Form
            else -> throw IllegalArgumentException("StudyCardType invalido")
        }
    }

    private fun String.toEvaluationMode(): EvaluationMode {
        return when (lowercase()) {
            "exact" -> EvaluationMode.Exact
            "flexible_text" -> EvaluationMode.FlexibleText
            "manual_self_check" -> EvaluationMode.ManualSelfCheck
            else -> throw IllegalArgumentException("EvaluationMode invalido")
        }
    }
}

@Serializable
private data class ExampleRegenerationResponseDto(
    val success: Boolean,
    val data: ExampleRegenerationDataDto? = null,
)

@Serializable
private data class FieldRegenerationResponseDto(
    val success: Boolean,
    val data: FieldRegenerationDataDto? = null,
)

@Serializable
private data class FieldRegenerationDataDto(
    @SerialName("why_useful") val whyUseful: String? = null,
    @SerialName("usage_pattern") val usagePattern: String? = null,
    @SerialName("common_mistake") val commonMistake: String? = null,
)

@Serializable
private data class ExampleRegenerationDataDto(
    @SerialName("example_sentence") val exampleSentence: String,
    @SerialName("example_translation") val exampleTranslation: String,
)

@Serializable
private data class ClozeRegenerationResponseDto(
    val success: Boolean,
    val data: ClozeRegenerationDataDto? = null,
)

@Serializable
private data class ClozeRegenerationDataDto(
    @SerialName("cloze_sentence") val clozeSentence: String,
)

@Serializable
private data class StudyCardRegenerationResponseDto(
    val success: Boolean,
    val data: StudyCardRegenerationDataDto? = null,
)

@Serializable
private data class StudyCardRegenerationDataDto(
    val card: RegeneratedStudyCardDto? = null,
)

@Serializable
private data class RegeneratedStudyCardDto(
    @SerialName("card_id") val cardId: String,
    @SerialName("card_type") val cardType: String,
    val prompt: String,
    @SerialName("expected_answer") val expectedAnswer: String,
    @SerialName("evaluation_mode") val evaluationMode: String,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("accepted_answers") val acceptedAnswers: List<String> = emptyList(),
    val hint: String = "",
    val explanation: String = "",
    @SerialName("source_field") val sourceField: String = "",
)
