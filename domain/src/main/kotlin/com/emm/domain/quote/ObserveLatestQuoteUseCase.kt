package com.emm.domain.quote

import kotlinx.coroutines.flow.Flow

class ObserveLatestQuoteUseCase(private val repository: QuoteRepository) {

    fun fetch(): Flow<List<Quote>> {
        return repository.lastQuote()
    }
}
