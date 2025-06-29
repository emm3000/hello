package com.emm.domain.quote

import kotlinx.coroutines.flow.Flow

interface QuoteRepository {

    suspend fun generate()

    fun lastQuote(): Flow<List<Quote>>

    fun allQuotes(): Flow<List<Quote>>
}