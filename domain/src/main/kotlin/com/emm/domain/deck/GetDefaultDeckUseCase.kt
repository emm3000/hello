package com.emm.domain.deck

import com.emm.domain.ids.DeckId

class GetDefaultDeckUseCase(
    private val repository: DefaultDeckSelectionRepository,
) {
    operator fun invoke(): DeckId? = repository.getDefaultDeckId()
}
