package com.emm.domain.deck

import com.emm.domain.ids.toDeckId

class GetDefaultDeckUseCase(
    private val repository: DefaultDeckSelectionRepository,
) {
    operator fun invoke(): String {
        val typedDeckId = repository.getDefaultDeckId().toDeckId()
        return typedDeckId.value
    }
}
