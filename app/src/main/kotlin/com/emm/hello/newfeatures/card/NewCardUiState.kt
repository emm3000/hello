package com.emm.hello.newfeatures.card

import com.emm.domain.deck.Deck
import com.emm.domain.flashcard.Flashcard
import com.emm.domain.flashcard.StaticCategories
import com.emm.domain.flashcard.TypeView
import com.emm.domain.flashcard.difficult
import com.emm.domain.flashcard.staticCategories

data class NewCardUiState(
    val word: String = "",
    val decks: List<Deck> = emptyList(),
    val deckSelected: Deck? = null,
    val isLoading: Boolean = false,
    val isCheck: Boolean = false,
    val category: StaticCategories = staticCategories.first(),
    val typeView: TypeView = TypeView.WordOrPhase,
    val difficulty: String = difficult.first(),
    val error: String? = null,
    val result: Flashcard? = null,
)
