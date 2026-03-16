package com.emm.domain.quote

class GenerateQuoteUseCase(private val repository: QuoteRepository) {

    suspend fun generateQuote() {
        repository.generate()
    }
}
