package com.emm.data.flashcard

import com.emm.data.flashcard.iadto.StoredNoteQualityCheckDto
import com.emm.data.flashcard.iadto.StoredStudyCardDto
import com.emm.domain.authoring.EncodedLearningArtifacts
import com.emm.domain.authoring.LearningNoteArtifactSerializer
import com.emm.domain.generation.GeneratedNoteQualityCheck
import com.emm.domain.generation.GeneratedStudyCard
import kotlinx.serialization.json.Json

class KotlinxLearningNoteArtifactSerializer(
    private val json: Json,
) : LearningNoteArtifactSerializer {

    override fun encode(
        studyCards: List<GeneratedStudyCard>,
        qualityChecks: List<GeneratedNoteQualityCheck>,
    ): EncodedLearningArtifacts = EncodedLearningArtifacts(
        studyCardsJson = json.encodeToString(studyCards.map { it.toDto() }),
        qualityChecksJson = json.encodeToString(qualityChecks.map { it.toDto() }),
    )

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
