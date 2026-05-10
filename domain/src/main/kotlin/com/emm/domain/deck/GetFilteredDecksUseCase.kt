package com.emm.domain.deck

import kotlinx.coroutines.flow.Flow

class GetFilteredDecksUseCase(private val repository: DeckRepository) {

    operator fun invoke(criteria: DeckSearchCriteria): Flow<List<Deck>> {
        return repository.observeFiltered(criteria)
    }
}
