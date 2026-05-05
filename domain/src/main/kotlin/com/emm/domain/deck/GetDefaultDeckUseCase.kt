package com.emm.domain.deck

import com.emm.domain.ids.toDeckId

class GetDefaultDeckUseCase(
    private val repository: DefaultDeckSelectionRepository,
) {
    operator fun invoke(): String {
        val rawDeckId = repository.getDefaultDeckId()
        if (rawDeckId.isBlank()) return ""

        val typedDeckId = rawDeckId.toDeckId()
        return typedDeckId.value
    }
}
