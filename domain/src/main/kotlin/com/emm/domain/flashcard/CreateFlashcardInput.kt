package com.emm.domain.flashcard

import com.emm.domain.generation.GeneratedNoteQualityCheck
import com.emm.domain.generation.GeneratedStudyCard
import com.emm.domain.ids.DeckId
import com.emm.domain.ids.FlashcardId

data class CreateFlashcardInput(
    val id: FlashcardId? = null,
    val deckId: DeckId,
    val word: String,
    val meaning: String,
    val translation: String,
    val phonetic: String,
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
    val studyCards: List<GeneratedStudyCard> = emptyList(),
    val qualityChecks: List<GeneratedNoteQualityCheck> = emptyList(),
    val enrichmentStatus: EnrichmentStatus = EnrichmentStatus.ENRICHED,
)

data class UpdateFlashcardInput(
    val flashcardId: FlashcardId,
    val word: String,
    val meaning: String,
    val translation: String,
    val phonetic: String = "",
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
    val examples: List<Example> = emptyList(),
)
