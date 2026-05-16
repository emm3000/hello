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

        /**
         * Construye una `Flashcard` validada vía los value objects de dominio
         * (`Expression`, `DefinitionEn`, `IntendedMeaningEs`). Los strings
         * resultantes quedan normalizados (trim + whitespace collapse).
         *
         * Path para crear flashcards desde **código nuevo** (use cases, previews,
         * mappers internos). NO se usa en la hidratación DB→domain porque ahí
         * los campos pueden venir vacíos por datos legacy.
         *
         * @throws IllegalArgumentException si `word`, `meaning` o `translation`
         * resultan blank tras normalizar (vía los VOs).
         */
        @Suppress("LongParameterList")
        fun create(
            id: FlashcardId,
            word: String,
            meaning: String,
            translation: String,
            phonetic: String = "",
            review: FlashcardReview,
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
