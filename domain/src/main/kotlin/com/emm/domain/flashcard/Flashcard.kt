package com.emm.domain.flashcard

import com.emm.domain.ids.FlashcardId
import com.emm.domain.time.Clock
import com.emm.domain.time.SystemClock

data class Flashcard(
    val id: FlashcardId,
    val word: String,
    val meaning: String,
    val translation: String,
    val examples: List<Example>,
    val phonetic: String,
    val review: FlashcardReview,
    val partOfSpeech: String = "",
    val noteType: String = "",
    val noteSummary: String = "",
    val register: String = "",
    val levelBand: String = "",
    val learningDomain: String = "",
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
) {

    companion object {

        fun empty(clock: Clock): Flashcard = Flashcard(
            id = FlashcardId.from("empty-flashcard"),
            word = "",
            meaning = "",
            translation = "",
            examples = emptyList(),
            phonetic = "",
            review = FlashcardReview.empty(clock),
        )

        val Empty: Flashcard
            get() = empty(SystemClock)
    }
}
