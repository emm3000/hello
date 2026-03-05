package com.emm.data.remote

import com.emm.data.Deck
import com.emm.data.DeckQueries
import com.emm.data.Flashcard
import com.emm.data.FlashcardExample
import com.emm.data.FlashcardExampleQueries
import com.emm.data.FlashcardQueries
import com.emm.data.FlashcardReview
import com.emm.data.FlashcardReviewQueries
import com.emm.data.HelloDb
import com.emm.data.Quote
import com.emm.data.QuotesQueries
import com.emm.data.SyncStatus
import com.emm.domain.backup.BackupRepository
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.internal.toLongOrDefault
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.time.Instant

class DefaultBackupRepository(
    db: HelloDb,
    private val androidId: String,
    private val backupService: ApiService,
    private val dataStore: DataStore,
    private val json: Json,
) : BackupRepository {

    private val decksDao: DeckQueries = db.deckQueries
    private val cardsDao: FlashcardQueries = db.flashcardQueries
    private val examplesDao: FlashcardExampleQueries = db.flashcardExampleQueries
    private val quotesDao: QuotesQueries = db.quotesQueries
    private val reviewDao: FlashcardReviewQueries = db.flashcardReviewQueries

    override suspend fun execute(force: Boolean) = withContext(Dispatchers.IO) {
        try {
            val (
                decks: List<Deck>,
                flashcards: List<Flashcard>,
                examples: List<FlashcardExample>,
                reviews: List<FlashcardReview>,
            ) = fetchAllDataInParallel()

            if (isEmpty(decks, flashcards, examples, reviews) || force) {
                populate()
                return@withContext Result.success(Unit)
            }

            val (
                pendingDecks: List<Deck>,
                pendingFlashCards: List<Flashcard>,
                pendingExamples: List<FlashcardExample>,
                pendingReviews: List<FlashcardReview>,
                pendingQuotes: List<Quote>,
            ) = fetchPendingSyncDataInParallel()

            val syncRequest = SyncRequest(
                androidId = androidId,
                decks = pendingDecks.map(::deckToDto),
                flashcards = pendingFlashCards.map(::flashcardToDto),
                flashcardExamples = pendingExamples.map(::exampleToDto),
                quotes = pendingQuotes.map(::quoteToDto),
                flashcardReviews = pendingReviews.map(::reviewToDto),
            )

            val response = backupService.createBackup(syncRequest)
            decksDao.markAsSynced(pendingDecks.map(Deck::id))
            cardsDao.markAsSynced(pendingFlashCards.map(Flashcard::id))
            examplesDao.markAsSynced(pendingExamples.map(FlashcardExample::id))
            quotesDao.markAsSynced(pendingQuotes.map(Quote::id))
            reviewDao.markAsSynced(pendingReviews.map(FlashcardReview::flashcardId))
            dataStore.markDate()
            dataStore.saveSuccess(json.encodeToString(response))

            return@withContext Result.success(Unit)
        } catch (e: SocketTimeoutException) {
            return@withContext Result.failure(e)
        } catch (e: HttpException) {
            dataStore.saveError(e)
            return@withContext Result.failure(e)
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    private fun isEmpty(
        decks: List<Deck>,
        flashcards: List<Flashcard>,
        examples: List<FlashcardExample>,
        reviews: List<FlashcardReview>,
    ): Boolean = decks.isEmpty() && flashcards.isEmpty() && examples.isEmpty() && reviews.isEmpty()

    private suspend fun fetchAllDataInParallel(): HolderOfDatabaseTables = coroutineScope {
        val decks: Deferred<List<Deck>> = async { decksDao.all().executeAsList() }
        val flashcards: Deferred<List<Flashcard>> = async { cardsDao.all().executeAsList() }
        val examples: Deferred<List<FlashcardExample>> = async { examplesDao.all().executeAsList() }
        val reviews: Deferred<List<FlashcardReview>> = async { reviewDao.all().executeAsList() }
        HolderOfDatabaseTables(
            decks = decks.await(),
            flashcards = flashcards.await(),
            examples = examples.await(),
            reviews = reviews.await(),
            quotes = emptyList(),
        )
    }

    private suspend fun fetchPendingSyncDataInParallel(): HolderOfDatabaseTables = coroutineScope {
        val decks = async { decksDao.pending().executeAsList() }
        val flashcards = async { cardsDao.pending().executeAsList() }
        val examples: Deferred<List<FlashcardExample>> = async { examplesDao.pending().executeAsList() }
        val quotes = async { quotesDao.pending().executeAsList() }
        val reviews = async { reviewDao.pending().executeAsList() }
        HolderOfDatabaseTables(
            decks = decks.await(),
            flashcards = flashcards.await(),
            examples = examples.await(),
            reviews = reviews.await(),
            quotes = quotes.await(),
        )
    }

    suspend fun populate() {
        val syncResponse: FetchSyncResponse = backupService.fetchSync(androidId)

        syncResponse.decks.forEach {
            decksDao.insert(
                id = it.id,
                name = it.name,
                description = it.description,
                createdAt = it.createdAt.toLongOrDefault(Instant.now().toEpochMilli()),
                updatedAt = it.updatedAt?.toLongOrDefault(Instant.now().toEpochMilli()) ?: Instant.now().toEpochMilli(),
                syncStatus = SyncStatus.Synced.name,
            )
        }

        syncResponse.flashcards.forEach {
            cardsDao.create(
                id = it.id,
                deckId = it.deckId,
                word = it.word,
                meaning = it.meaning,
                translation = it.translation,
                phonetic = it.phonetic,
                partOfSpeech = "",
                type = "",
                note = "",
                createdAt = it.createdAt.toLongOrDefault(Instant.now().toEpochMilli()),
                updatedAt = it.updatedAt?.toLongOrDefault(Instant.now().toEpochMilli()) ?: Instant.now().toEpochMilli(),
                syncStatus = SyncStatus.Synced.name,
            )
        }

        syncResponse.flashcardExamples.forEach {
            examplesDao.insert(
                id = it.id,
                flashcardId = it.flashcardId,
                text = it.text,
                translation = it.translation,
                type = it.type,
                createdAt = it.createdAt?.toLongOrDefault(Instant.now().toEpochMilli()) ?: Instant.now().toEpochMilli(),
                updatedAt = it.updatedAt?.toLongOrDefault(Instant.now().toEpochMilli()) ?: Instant.now().toEpochMilli(),
                syncStatus = SyncStatus.Synced.name,
            )
        }

        syncResponse.quotes.forEach {
            quotesDao.insert(
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
                category = it.category.orEmpty(),
                createdAt = it.createdAt.toLongOrDefault(Instant.now().toEpochMilli()),
                updatedAt = it.updatedAt?.toLongOrDefault(Instant.now().toEpochMilli()) ?: Instant.now().toEpochMilli(),
                syncStatus = SyncStatus.Synced.name,
            )
        }

        syncResponse.flashcardReviews.forEach {
            reviewDao.upsertFlashcardReview(
                flashcardId = it.flashcardId,
                lastReviewedAt = it.lastReviewedAt,
                nextReviewAt = it.nextReviewAt,
                easeFactor = it.easeFactor,
                interval = it.interval,
                repetitions = it.repetitions,
                lapses = it.lapses,
                createdAt = it.createdAt.toLongOrDefault(Instant.now().toEpochMilli()),
                updatedAt = it.updatedAt.toLongOrDefault(Instant.now().toEpochMilli()),
                syncStatus = SyncStatus.Synced.name,
            )
        }
    }
}
