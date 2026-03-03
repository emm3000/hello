package com.emm.hello.newfeatures.card

import com.emm.domain.deck.Deck
import com.emm.domain.flashcard.StaticCategories
import com.emm.domain.flashcard.TypeView

sealed interface NewCardAction {

    data class WordChanged(val word: String) : NewCardAction

    data class DeckSelected(val deck: Deck) : NewCardAction

    data class CheckChanged(val checked: Boolean) : NewCardAction

    data class CategorySelected(val category: StaticCategories) : NewCardAction

    data class DifficultySelected(val difficulty: String) : NewCardAction

    data class TypeViewSelected(val typeView: TypeView) : NewCardAction

    data object GenerateClicked : NewCardAction

    data object SaveClicked : NewCardAction

    data object SuccessConsumed : NewCardAction
}
