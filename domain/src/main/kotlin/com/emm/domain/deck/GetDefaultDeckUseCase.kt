package com.emm.domain.deck

class GetDefaultDeckUseCase(
    private val repository: DefaultDeckSelectionRepository,
) {
    operator fun invoke(): String {
        return repository.getDefaultDeckId()
    }
}
