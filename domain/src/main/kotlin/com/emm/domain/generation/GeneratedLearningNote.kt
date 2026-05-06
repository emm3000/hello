package com.emm.domain.generation

import com.emm.domain.flashcard.DefinitionEn
import com.emm.domain.flashcard.Expression
import com.emm.domain.flashcard.IntendedMeaningEs

data class GeneratedLearningNote(
    val noteId: String,
    val noteType: LearningNoteType,
    val expression: Expression,
    val intendedMeaningEs: IntendedMeaningEs,
    val simpleDefinitionEn: DefinitionEn,
    val partOfSpeech: PartOfSpeechTag,
    val register: RegisterPreference,
    val levelBand: LevelBand,
    val domain: LearningDomain,
    val whyUseful: String,
    val exampleSentence: String,
    val exampleTranslation: String,
    val cards: List<GeneratedStudyCard>,
    val qualityChecks: List<GeneratedNoteQualityCheck>,
    val lemma: String = "",
    val ipa: String = "",
    val usagePattern: String = "",
    val irregularForms: List<String> = emptyList(),
    val collocations: List<String> = emptyList(),
    val commonMistake: String = "",
    val confusableWith: List<String> = emptyList(),
    val clozeSentence: String = "",
    val sourceContext: String = "",
    val warnings: List<String> = emptyList(),
)
