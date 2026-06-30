package com.emm.hello.newfeatures.study

import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.FsrsCard
import com.emm.domain.generation.GeneratedStudyCard
import com.emm.domain.ids.FlashcardId
import com.emm.domain.study.StudyFlashcard

data class StudySessionItem(
    val flashcardId: FlashcardId,
    val review: FsrsCard,
    val studyCard: GeneratedStudyCard,
    val word: String,
    val phonetic: String,
    val meaning: String,
    val translation: String,
    val usagePattern: String = "",
    val whyUseful: String = "",
    val sourceContext: String = "",
    val irregularForms: List<String> = emptyList(),
) {
    val flashcard: Flashcard
        get() = Flashcard(
            id = flashcardId,
            word = word,
            meaning = meaning,
            translation = translation,
            examples = emptyList(),
            phonetic = phonetic,
            review = review,
            usagePattern = usagePattern,
            whyUseful = whyUseful,
            sourceContext = sourceContext,
            irregularForms = irregularForms,
        )
}

internal fun StudyFlashcard.toStudySessionItems(): List<StudySessionItem> {
    return studyCards
        .filter { it.isActive }
        .map { card ->
            StudySessionItem(
                flashcardId = flashcardId,
                review = review,
                studyCard = card,
                word = word,
                phonetic = phonetic,
                meaning = meaning,
                translation = translation,
                usagePattern = usagePattern,
                whyUseful = whyUseful,
                sourceContext = sourceContext,
                irregularForms = irregularForms,
            )
        }
}
