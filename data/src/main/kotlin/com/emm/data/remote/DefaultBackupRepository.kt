package com.emm.data.remote

import com.emm.data.Deck
import com.emm.data.DeckQueries
import com.emm.data.Flashcard
import com.emm.data.FlashcardExample
import com.emm.data.FlashcardExampleQueries
import com.emm.data.FlashcardQueries
import com.emm.data.HelloDb
import com.emm.data.Quote
import com.emm.data.QuotesQueries
import com.emm.domain.backup.BackupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import retrofit2.HttpException

class DefaultBackupRepository(
    db: HelloDb,
    private val androidId: String,
    private val backupService: BackupApi,
    private val dataStore: DataStore,
    private val json: Json,
) : BackupRepository {

    private val decksDao: DeckQueries = db.deckQueries
    private val cardsDao: FlashcardQueries = db.flashcardQueries
    private val examplesDao: FlashcardExampleQueries = db.flashcardExampleQueries
    private val quotesDao: QuotesQueries = db.quotesQueries

    override suspend fun execute() = withContext(Dispatchers.IO) {
        try {
            val (decks, flashcards, examples, quotes) = fetchAllDataInParallel()

            if (isEmpty(decks, flashcards, examples, quotes)) {
                return@withContext Result.success(Unit)
            }

            val helloDto = HelloDto(
                androidId = androidId,
                decks = decks.map(::deckToDto),
                flashcards = flashcards.map(::flashcardToDto),
                flashcardExamples = examples.map(::exampleToDto),
                quotes = quotes.map(::quoteToDto)
            )

            val currentChecksum: String = json.encodeToString(helloDto).toSha256()
            val lastChecksum = dataStore.checksum

            if (currentChecksum == lastChecksum) {
                return@withContext Result.success(Unit)
            }

            dataStore.checksum = currentChecksum

            val response = backupService.backup(helloDto)
            dataStore.markDate()
            dataStore.saveSuccess(json.encodeToString(response))

            return@withContext Result.success(Unit)
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
        quotes: List<Quote>,
    ): Boolean = decks.isEmpty() && flashcards.isEmpty() && examples.isEmpty() && quotes.isEmpty()

    private suspend fun fetchAllDataInParallel(): Quad<List<Deck>, List<Flashcard>, List<FlashcardExample>, List<Quote>> = coroutineScope {
        val decks = async { decksDao.all().executeAsList() }
        val flashcards = async { cardsDao.all().executeAsList() }
        val examples = async { examplesDao.all().executeAsList() }
        val quotes = async { quotesDao.all().executeAsList() }
        Quad(decks.await(), flashcards.await(), examples.await(), quotes.await())
    }
}