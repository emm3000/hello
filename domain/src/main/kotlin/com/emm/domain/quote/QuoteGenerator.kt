package com.emm.domain.quote

class QuoteGenerator(private val repository: QuoteRepository) {

    suspend fun generateQuote() {
        repository.generate()
    }
}
