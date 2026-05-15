package com.emm.domain.authoring

import com.emm.domain.generation.GeneratedNoteQualityCheck
import com.emm.domain.generation.GeneratedStudyCard

data class EncodedLearningArtifacts(
    val studyCardsJson: String,
    val qualityChecksJson: String,
)

interface LearningNoteArtifactSerializer {
    fun encode(
        studyCards: List<GeneratedStudyCard>,
        qualityChecks: List<GeneratedNoteQualityCheck>,
    ): EncodedLearningArtifacts
}
