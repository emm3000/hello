package com.emm.data.quote

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.emm.data.HelloDb
import com.emm.data.QuotesQueries
import com.emm.data.flashcard.GeminiService
import com.emm.data.flashcard.Prompt
import com.emm.data.localfirst.LocalDeviceIdentityProvider
import com.emm.data.localfirst.LocalFirstWrite
import com.emm.data.localfirst.OperationLogWriter
import com.emm.domain.quote.Quote
import com.emm.domain.quote.QuoteRepository
import com.emm.domain.sync.OperationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.util.UUID

typealias QuoteEntity = com.emm.data.Quote

@LocalFirstWrite
class DefaultQuoteRepository(
    private val db: HelloDb,
    private val geminiApi: GeminiService,
    private val json: Json,
    private val operationLogWriter: OperationLogWriter,
    private val localDeviceIdentityProvider: LocalDeviceIdentityProvider,
) : QuoteRepository {

    private val dao: QuotesQueries = db.quotesQueries

    override suspend fun generate() = withContext(Dispatchers.IO) {
        val quotePrompt: String = Prompt.quotePrompt2()
        val process: String = geminiApi.process(quotePrompt)
        val quote: QuoteResponse = parseQuoteResponse(process, json) ?: return@withContext
        val now = Instant.now().toEpochMilli()
        val quoteId = UUID.randomUUID().toString()
        val deviceId = localDeviceIdentityProvider.getOrCreateDeviceId()

        db.transaction {
            val payloadJson = buildJsonObject {
                put("entityId", quoteId)
                put("operationType", OperationType.Create.name)
                put("title", quote.title)
                put("phrase", quote.phrase)
                put("description", quote.description)
                put("translation", quote.translation)
                put("example", quote.example)
                put("context", quote.context)
                put("pronunciation", quote.pronunciation)
                put("formality", quote.formality)
                put("tags", quote.tags.joinToString("|"))
                put("category", quote.category)
                put("createdAt", now)
                put("updatedAt", now)
            }.toString()
            val lamport = operationLogWriter.appendOperation(
                entityType = "quote",
                entityId = quoteId,
                operationType = OperationType.Create,
                payloadJson = payloadJson,
                originDeviceId = deviceId,
                createdAt = now,
            )
            dao.insert(
                id = quoteId,
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
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
                originDeviceId = deviceId,
                lastModifiedByDeviceId = deviceId,
                versionLamport = lamport,
            )
        }
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
