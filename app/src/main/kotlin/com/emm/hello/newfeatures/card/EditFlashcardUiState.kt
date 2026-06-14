package com.emm.hello.newfeatures.card

import androidx.annotation.StringRes
import com.emm.domain.flashcard.Example
import com.emm.hello.core.mvi.MviState

data class EditFlashcardUiState(
    val isLoading: Boolean = true,
    val word: String = "",
    val meaning: String = "",
    val translation: String = "",
    val phonetic: String = "",
    val partOfSpeech: String = "",
    val examples: List<Example> = emptyList(),
    @param:StringRes val wordError: Int? = null,
    @param:StringRes val meaningError: Int? = null,
    val isSubmitting: Boolean = false,
    val isDeleteConfirmationVisible: Boolean = false,
) : MviState {
    val isValid: Boolean
        get() = word.isNotBlank() && meaning.isNotBlank() && wordError == null && meaningError == null
}
