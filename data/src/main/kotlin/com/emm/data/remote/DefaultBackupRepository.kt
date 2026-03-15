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
import com.emm.data.localfirst.INITIAL_LAMPORT_VERSION
import com.emm.data.localfirst.LocalDeviceIdentityProvider
import com.emm.data.localfirst.REMOTE_DEVICE_ID
import com.emm.domain.backup.BackupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.internal.toLongOrDefault
import java.time.Instant

class DefaultBackupRepository(
    db: HelloDb,
    private val localDeviceIdentityProvider: LocalDeviceIdentityProvider,
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
            val decks: List<Deck> = decksDao.all().executeAsList()
            val flashcards: List<Flashcard> = cardsDao.all().executeAsList()
            val examples: List<FlashcardExample> = examplesDao.all().executeAsList()
            val reviews: List<FlashcardReview> = reviewDao.all().executeAsList()

            if (isEmpty(decks, flashcards, examples, reviews) || force) {
                populate()
                return@withContext Result.success(Unit)
            }

            dataStore.markDate()
            dataStore.saveSuccess("Local-first mode: legacy backup push disabled")

            return@withContext Result.success(Unit)
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

    suspend fun populate() {
        val deviceId: String = localDeviceIdentityProvider.getOrCreateDeviceId()
        val syncResponse: FetchSyncResponse = backupService.fetchSync(deviceId)

        syncResponse.decks.forEach {
            decksDao.insert(
                id = it.id,
                name = it.name,
                description = it.description,
                createdAt = it.createdAt.toLongOrDefault(Instant.now().toEpochMilli()),
                updatedAt = it.updatedAt?.toLongOrDefault(Instant.now().toEpochMilli()) ?: Instant.now().toEpochMilli(),
                deletedAt = null,
                originDeviceId = REMOTE_DEVICE_ID,
                lastModifiedByDeviceId = REMOTE_DEVICE_ID,
                versionLamport = INITIAL_LAMPORT_VERSION,
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
                deletedAt = null,
                originDeviceId = REMOTE_DEVICE_ID,
                lastModifiedByDeviceId = REMOTE_DEVICE_ID,
                versionLamport = INITIAL_LAMPORT_VERSION,
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
                deletedAt = null,
                originDeviceId = REMOTE_DEVICE_ID,
                lastModifiedByDeviceId = REMOTE_DEVICE_ID,
                versionLamport = INITIAL_LAMPORT_VERSION,
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
                deletedAt = null,
                originDeviceId = REMOTE_DEVICE_ID,
                lastModifiedByDeviceId = REMOTE_DEVICE_ID,
                versionLamport = INITIAL_LAMPORT_VERSION,
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
            )
        }
    }
}
