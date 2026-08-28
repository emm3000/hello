package com.emm.hello.newfeatures.card

import androidx.annotation.StringRes
import com.emm.hello.core.mvi.MviState

data class EditFlashcardUiState(
    val flashcardId: String = "",
    val isLoading: Boolean = true,
    val word: String = "",
    val translation: String = "",
    val exampleText: String = "",
    val exampleTranslation: String = "",
    val partOfSpeech: String = "",
    val phonetic: String = "",
    @param:StringRes val wordError: Int? = null,
    val isSubmitting: Boolean = false,
    val isDeleteConfirmationVisible: Boolean = false,
) : MviState {
    val isValid: Boolean
        get() = word.isNotBlank() && wordError == null
}
