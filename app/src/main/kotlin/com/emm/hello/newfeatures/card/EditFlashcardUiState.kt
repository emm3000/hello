package com.emm.hello.newfeatures.card

import com.emm.domain.flashcard.Example

data class EditFlashcardUiState(
    val isLoading: Boolean = true,
    val word: String = "",
    val meaning: String = "",
    val translation: String = "",
    val phonetic: String = "",
    val partOfSpeech: String = "",
    val examples: List<Example> = emptyList(),
    val wordError: String? = null,
    val meaningError: String? = null,
    val isSubmitting: Boolean = false,
) {
    val isValid: Boolean
        get() = word.isNotBlank() && meaning.isNotBlank() && wordError == null && meaningError == null
}