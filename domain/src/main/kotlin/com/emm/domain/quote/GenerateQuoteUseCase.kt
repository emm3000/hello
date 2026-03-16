package com.emm.domain.quote

class GenerateQuoteUseCase(private val repository: QuoteRepository) {

    suspend operator fun invoke() {
        repository.generate()
    }
}
