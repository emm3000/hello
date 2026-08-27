package com.emm.domain.library

import com.emm.domain.ids.DeckId
import com.emm.domain.text.searchNormalized
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SearchLibraryUseCase(private val repository: LibraryRepository) {

    operator fun invoke(query: String, deckId: DeckId? = null): Flow<List<LibraryFlashcard>> {
        val needle: String = query.searchNormalized()
        return repository.observeLibrary().map { cards -> cards.matching(needle, deckId) }
    }

    private fun List<LibraryFlashcard>.matching(needle: String, deckId: DeckId?): List<LibraryFlashcard> {
        val scoped: List<LibraryFlashcard> = if (deckId == null) this else filter { it.deckId == deckId }
        if (needle.isEmpty()) return scoped
        return scoped.filter { it.contains(needle) }
    }

    private fun LibraryFlashcard.contains(needle: String): Boolean =
        word.searchNormalized().contains(needle) ||
            translation.searchNormalized().contains(needle) ||
            meaning.searchNormalized().contains(needle)
}
