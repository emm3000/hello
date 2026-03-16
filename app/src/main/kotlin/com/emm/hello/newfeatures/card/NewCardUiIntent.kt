package com.emm.hello.newfeatures.card

import com.emm.domain.deck.Deck
import com.emm.domain.flashcard.StaticCategories
import com.emm.domain.flashcard.TypeView

sealed interface NewCardUiIntent {

    data class WordChanged(val word: String) : NewCardUiIntent

    data class DeckSelected(val deck: Deck) : NewCardUiIntent

    data class CheckChanged(val checked: Boolean) : NewCardUiIntent

    data class CategorySelected(val category: StaticCategories) : NewCardUiIntent

    data class DifficultySelected(val difficulty: String) : NewCardUiIntent

    data class TypeViewSelected(val typeView: TypeView) : NewCardUiIntent

    data object GenerateClicked : NewCardUiIntent

    data object SaveClicked : NewCardUiIntent
}
