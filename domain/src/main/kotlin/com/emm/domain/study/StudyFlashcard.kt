package com.emm.domain.study

import com.emm.domain.flashcard.FsrsCard
import com.emm.domain.generation.GeneratedStudyCard
import com.emm.domain.ids.FlashcardId

data class StudyFlashcard(
    val flashcardId: FlashcardId,
    val word: String,
    val phonetic: String,
    val meaning: String,
    val translation: String,
    val review: FsrsCard,
    val studyCards: List<GeneratedStudyCard>,
    val usagePattern: String = "",
    val whyUseful: String = "",
    val sourceContext: String = "",
    val irregularForms: List<String> = emptyList(),
    val partOfSpeech: String = "",
    val example: String = "",
    val exampleTranslation: String = "",
)
