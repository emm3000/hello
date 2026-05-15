package com.emm.domain.authoring

import com.emm.domain.generation.GeneratedNoteQualityCheck
import com.emm.domain.generation.GeneratedStudyCard

internal class FakeLearningNoteArtifactSerializer : LearningNoteArtifactSerializer {
    override fun encode(
        studyCards: List<GeneratedStudyCard>,
        qualityChecks: List<GeneratedNoteQualityCheck>,
    ): EncodedLearningArtifacts = EncodedLearningArtifacts(
        studyCardsJson = "[cards:${studyCards.size}]",
        qualityChecksJson = "[checks:${qualityChecks.size}]",
    )
}

internal fun testMapper(): GeneratedLearningNoteMapper =
    GeneratedLearningNoteMapper(FakeLearningNoteArtifactSerializer())
