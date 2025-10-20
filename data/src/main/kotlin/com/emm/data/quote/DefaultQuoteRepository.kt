package com.emm.data.quote

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.emm.data.HelloDb
import com.emm.data.QuotesQueries
import com.emm.data.SyncStatus
import com.emm.data.flashcard.GeminiService
import com.emm.data.flashcard.Prompt
import com.emm.domain.quote.Quote
import com.emm.domain.quote.QuoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID

typealias QuoteEntity = com.emm.data.Quote

class DefaultQuoteRepository(
    db: HelloDb,
    private val geminiApi: GeminiService,
    private val json: Json,
    private val synchronizer: QuoteSynchronizer,
) : QuoteRepository {

    private val dao: QuotesQueries = db.quotesQueries

    override suspend fun generate() = withContext(Dispatchers.IO) {
        val quotePrompt: String = Prompt.quotePrompt2()
        val process: String = geminiApi.process(quotePrompt)
        val quote: QuoteResponse = parseQuoteResponse(process, json) ?: return@withContext
        dao.insert(
            id = UUID.randomUUID().toString(),
            title = quote.title,
            phrase = quote.phrase,
            description = quote.description,
            translation = quote.translation,
            example = quote.example,
            context = quote.context,
            pronunciation = quote.pronunciation,
            formality = quote.formality,
            tags = quote.tags.joinToString("|"),
            category = quote.category,
            createdAt = Instant.now().toEpochMilli(),
            updatedAt = Instant.now().toEpochMilli(),
            syncStatus = SyncStatus.Pending.name,
        )
        synchronizer.synchronize()
    }

    override fun lastQuote(): Flow<List<Quote>> = dao
        .lastInsertedQuote()
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map(::toDomain)

    override fun allQuotes(): Flow<List<Quote>> = dao
        .all2()
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map {
            it.map { entity ->
                Quote(
                    id = entity.id,
                    title = entity.title,
                    phrase = entity.phrase,
                    description = entity.description,
                    translation = entity.translation,
                    example = entity.example,
                    context = entity.context,
                    pronunciation = entity.pronunciation,
                    formality = entity.formality,
                    tags = entity.tags.split("|").map(String::trim),
                    category = entity.category,
                    hasCard = entity.hasCard,
                )
            }

        }

    private fun toDomain(quotes: List<QuoteEntity>): List<Quote> = quotes.map(QuoteEntity::toDomain)
}