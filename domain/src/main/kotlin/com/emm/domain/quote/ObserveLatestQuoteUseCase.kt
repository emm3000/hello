package com.emm.domain.quote

import kotlinx.coroutines.flow.Flow

class ObserveLatestQuoteUseCase(private val repository: QuoteRepository) {

    operator fun invoke(): Flow<List<Quote>> {
        return repository.lastQuote()
    }
}
