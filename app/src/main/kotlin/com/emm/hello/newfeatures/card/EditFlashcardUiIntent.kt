package com.emm.hello.newfeatures.card

import com.emm.domain.flashcard.Example

sealed interface EditFlashcardUiIntent {
    data class WordChanged(val word: String) : EditFlashcardUiIntent
    data class MeaningChanged(val meaning: String) : EditFlashcardUiIntent
    data class TranslationChanged(val translation: String) : EditFlashcardUiIntent
    data class PhoneticChanged(val phonetic: String) : EditFlashcardUiIntent
    data class PartOfSpeechChanged(val partOfSpeech: String) : EditFlashcardUiIntent
    data class ExampleTextChanged(val index: Int, val text: String) : EditFlashcardUiIntent
    data class ExampleTranslationChanged(val index: Int, val translation: String) : EditFlashcardUiIntent
    data object AddExample : EditFlashcardUiIntent
    data class RemoveExample(val index: Int) : EditFlashcardUiIntent
    data object Submit : EditFlashcardUiIntent
}