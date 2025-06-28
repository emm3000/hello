package com.emm.data.remote

import android.content.Context
import android.content.SharedPreferences
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
import java.time.LocalDateTime

class DefaultBackupRepository(
    db: HelloDb,
    private val sharedPreferences: SharedPreferences,
    private val backupService: BackupApi,
    private val context: Context,
    private val json: Json,
) : BackupRepository {

    private val decksDao: DeckQueries = db.deckQueries
    private val cardsDao: FlashcardQueries = db.flashcardQueries
    private val examplesDao: FlashcardExampleQueries = db.flashcardExampleQueries
    private val quotesDao: QuotesQueries = db.quotesQueries

    private val editor = sharedPreferences.edit()

    override suspend fun execute() = withContext(Dispatchers.IO) {
        try {
            val (decks, flashcards, examples, quotes) = fetchAllDataInParallel()

            if (isEmpty(decks, flashcards, examples, quotes)) {
                return@withContext Result.success(Unit)
            }

            val androidId: String = androidId(context)

            val helloDto = HelloDto(
                androidId = androidId,
                decks = decks.map(::deckToDto),
                flashcards = flashcards.map(::flashcardToDto),
                flashcardExamples = examples.map(::exampleToDto),
                quotes = quotes.map(::quoteToDto)
            )

            val currentChecksum: String = json.encodeToString(helloDto).toSha256()
            val lastChecksum = sharedPreferences.getString(LAST_CHECKSUM, null).orEmpty()

            if (currentChecksum == lastChecksum) {
                return@withContext Result.success(Unit)
            }

            editor.putString(LAST_CHECKSUM, currentChecksum).apply()

            val response = backupService.backup(helloDto)

            editor.apply {
                putString(SUCCESS_KEY, json.encodeToString(response))
                putString(DATE_KEY, LocalDateTime.now().toString())
                apply()
            }
            return@withContext Result.success(Unit)
        } catch (e: HttpException) {
            saveError(e)
            return@withContext Result.failure(e)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Result.failure(e)
        }
    }

    private fun isEmpty(
        decks: List<Deck>,
        flashcards: List<Flashcard>,
        examples: List<FlashcardExample>,
        quotes: List<Quote>,
    ): Boolean = decks.isEmpty() && flashcards.isEmpty() && examples.isEmpty() && quotes.isEmpty()

    private fun saveError(e: HttpException) {
        try {
            e.response()?.errorBody()?.string()?.let {
                val decodedErrorResponse: ExceptionResponse = json.decodeFromString<ExceptionResponse>(it)
                val encodedErrorResponse: String = json.encodeToString(decodedErrorResponse)
                editor.putString(ERROR_KEY, encodedErrorResponse).apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun fetchAllDataInParallel(): Quad<List<Deck>, List<Flashcard>, List<FlashcardExample>, List<Quote>> = coroutineScope {
        val decks = async { decksDao.all().executeAsList() }
        val flashcards = async { cardsDao.all().executeAsList() }
        val examples = async { examplesDao.all().executeAsList() }
        val quotes = async { quotesDao.all().executeAsList() }
        Quad(decks.await(), flashcards.await(), examples.await(), quotes.await())
    }

    companion object {

        const val ERROR_KEY = "ERROR_KEY"
        const val SUCCESS_KEY = "SUCCESS_KEY"
        const val DATE_KEY = "DATE_KEY"
        const val LAST_CHECKSUM = "LAST_CHECKSUM"
    }
}