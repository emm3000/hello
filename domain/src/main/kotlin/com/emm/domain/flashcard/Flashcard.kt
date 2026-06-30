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
    val review: FsrsCard,
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

        @Suppress("LongParameterList")
        fun create(
            id: FlashcardId,
            word: String,
            meaning: String,
            translation: String,
            phonetic: String = "",
            review: FsrsCard,
            examples: List<Example> = emptyList(),
            partOfSpeech: String = "",
            noteType: String = "",
            noteSummary: String = "",
            register: String = "",
            levelBand: String = "",
            learningDomain: String = "",
            lemma: String = "",
            whyUseful: String = "",
            usagePattern: String = "",
            irregularForms: List<String> = emptyList(),
            collocations: List<String> = emptyList(),
            commonMistake: String = "",
            confusableWith: List<String> = emptyList(),
            clozeSentence: String = "",
            sourceContext: String = "",
            warnings: List<String> = emptyList(),
        ): Flashcard = Flashcard(
            id = id,
            word = Expression.from(word).value,
            meaning = DefinitionEn.from(meaning).value,
            translation = IntendedMeaningEs.from(translation).value,
            phonetic = phonetic,
            review = review,
            examples = examples,
            partOfSpeech = partOfSpeech,
            noteType = noteType,
            noteSummary = noteSummary,
            register = register,
            levelBand = levelBand,
            learningDomain = learningDomain,
            lemma = lemma,
            whyUseful = whyUseful,
            usagePattern = usagePattern,
            irregularForms = irregularForms,
            collocations = collocations,
            commonMistake = commonMistake,
            confusableWith = confusableWith,
            clozeSentence = clozeSentence,
            sourceContext = sourceContext,
            warnings = warnings,
        )

        fun empty(clock: Clock): Flashcard {
            val id = FlashcardId.from("empty-flashcard")
            return Flashcard(
                id = id,
                word = "",
                meaning = "",
                translation = "",
                examples = emptyList(),
                phonetic = "",
                review = FsrsCard.new(id, clock),
            )
        }

        val Empty: Flashcard
            get() = empty(SystemClock)
    }
}
