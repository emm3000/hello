package com.emm.domain.flashcard

data class Flashcard(
    val id: String,
    val word: String,
    val meaning: String,
    val translation: String,
    val examples: List<Example>,
    val phonetic: String,
    val review: FlashcardReview,
    val partOfSpeech: String = "",
    val type: String = "",
    val note: String = "",
    val register: String = "",
    val levelBand: String = "",
    val domain: String = "",
    val lemma: String = "",
    val whyUseful: String = "",
    val usagePattern: String = "",
    val irregularForms: List<String> = emptyList(),
    val collocations: List<String> = emptyList(),
    val commonMistake: String = "",
    val confusableWith: List<String> = emptyList(),
    val clozeSentence: String = "",
    val sourceContext: String = "",
    val warnings: List<String> = emptyList(),
    val studyCards: List<GeneratedStudyCard> = emptyList(),
    val qualityChecks: List<GeneratedNoteQualityCheck> = emptyList(),
) {

    companion object {

        val Empty: Flashcard
            get() = Flashcard(
                id = "",
                word = "",
                meaning = "",
                translation = "",
                examples = emptyList(),
                phonetic = "",
                review = FlashcardReview.Empty,
            )
    }
}
