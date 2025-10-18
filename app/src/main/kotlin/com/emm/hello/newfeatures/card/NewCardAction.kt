package com.emm.hello.newfeatures.card

import com.emm.domain.deck.Deck
import com.emm.domain.flashcard.StaticCategories
import com.emm.domain.flashcard.TypeView

sealed interface NewCardAction {

    data class OnWordChanged(val word: String) : NewCardAction

    data class OnDeckSelected(val deck: Deck) : NewCardAction

    data class OnCheckChanged(val checked: Boolean) : NewCardAction

    data class OnCategorySelected(val category: StaticCategories) : NewCardAction

    data class OnDifficultySelected(val difficulty: String) : NewCardAction

    data class OnTypeViewSelected(val typeView: TypeView) : NewCardAction

    object OnGenerateClicked : NewCardAction

    object OnSaveClicked : NewCardAction
}