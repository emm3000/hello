package com.emm.hello.newfeatures.card

import com.emm.hello.core.mvi.MviIntent

sealed interface EditFlashcardUiIntent : MviIntent {
    data class WordChanged(val word: String) : EditFlashcardUiIntent
    data class TranslationChanged(val translation: String) : EditFlashcardUiIntent
    data class ExampleTextChanged(val text: String) : EditFlashcardUiIntent
    data class ExampleTranslationChanged(val translation: String) : EditFlashcardUiIntent
    data class PartOfSpeechChanged(val partOfSpeech: String) : EditFlashcardUiIntent
    data class PhoneticChanged(val phonetic: String) : EditFlashcardUiIntent
    data object CloseClicked : EditFlashcardUiIntent
    data object Submit : EditFlashcardUiIntent
    data object DeleteFlashcard : EditFlashcardUiIntent
    data object ConfirmDeleteFlashcard : EditFlashcardUiIntent
    data object DismissDeleteFlashcard : EditFlashcardUiIntent
}
