package com.emm.domain.deck

class GetDefaultDeckUseCase(
    private val repository: DefaultDeckSelectionRepository,
) {
    fun execute(): String {
        return repository.getDefaultDeckId()
    }
}
