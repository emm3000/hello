package com.emm.data.flashcard

import com.emm.data.flashcard.iadto.StoredNoteQualityCheckDto
import com.emm.data.flashcard.iadto.StoredStudyCardDto
import com.emm.domain.generation.GeneratedNoteQualityCheck
import com.emm.domain.generation.GeneratedStudyCard
import kotlinx.serialization.json.Json

/**
 * Encoder interno de `:data` que convierte las listas tipadas del domain
 * (`List<GeneratedStudyCard>`, `List<GeneratedNoteQualityCheck>`) en los
 * JSON strings que SQLDelight persiste. Vive 100% en `:data` para que
 * el domain no sepa que existe JSON.
 */
internal class LearningNoteArtifactEncoder(private val json: Json) {

    fun encodeStudyCards(cards: List<GeneratedStudyCard>): String =
        json.encodeToString(cards.map { it.toDto() })

    fun encodeQualityChecks(checks: List<GeneratedNoteQualityCheck>): String =
        json.encodeToString(checks.map { it.toDto() })

    private fun GeneratedStudyCard.toDto(): StoredStudyCardDto = StoredStudyCardDto(
        cardId = cardId,
        cardType = cardType.name,
        prompt = prompt,
        expectedAnswer = expectedAnswer,
        evaluationMode = evaluationMode.name,
        isActive = isActive,
        acceptedAnswers = acceptedAnswers,
        hint = hint,
        explanation = explanation,
        sourceField = sourceField,
    )

    private fun GeneratedNoteQualityCheck.toDto(): StoredNoteQualityCheckDto = StoredNoteQualityCheckDto(
        code = code.name,
        passed = passed,
        message = message,
    )
}
