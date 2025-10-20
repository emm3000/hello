package com.emm.data.quote

import android.content.Context
import com.emm.data.HelloDb
import com.emm.data.QuotesQueries
import com.emm.data.deck.RemoteDataSource

class QuoteSynchronizer(
    db: HelloDb,
    private val remote: RemoteDataSource,
    private val context: Context,
) {

    private val qq: QuotesQueries = db.quotesQueries

    suspend fun execute() {
        val quotes: List<QuoteEntity> = qq.pending().executeAsList()

        if (quotes.isEmpty()) return

        val quoteCreationRequests: List<CreateQuoteRequest> = quotes.map {
            CreateQuoteRequest(
                id = it.id,
                title = it.title,
                phrase = it.phrase,
                description = it.description,
                translation = it.translation,
                example = it.example,
                context = it.context,
                pronunciation = it.pronunciation,
                formality = it.formality,
                tags = it.tags,
                category = it.category,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt,
            )
        }
        remote.createQuote(quoteCreationRequests)
        val quoteIdsToMarkAsSynced = quotes.map(QuoteEntity::id)
        qq.markAsSynced(quoteIdsToMarkAsSynced)
    }

    fun synchronize() {
        QuoteWorker.initialize(context)
    }
}