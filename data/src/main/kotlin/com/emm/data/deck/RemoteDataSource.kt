package com.emm.data.deck

import android.util.Log
import com.emm.data.flashcard.CreateExampleRequest
import com.emm.data.flashcard.CreateFlashcardRequest
import com.emm.data.flashcard.CreateFlashcardReviewRequest
import com.emm.data.quote.CreateQuoteRequest
import com.emm.data.remote.ApiService
import com.emm.data.remote.DataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class RemoteDataSource(
    private val apiService: ApiService,
    private val json: Json,
    private val processor: RequestDataProcessor,
    private val dataStore: DataStore,
) {

    suspend fun createDeck(decks: List<CreateDeckRequest>) {
        apiService.createDecks(decks)
    }

    suspend fun createFlashcard(flashcards: List<CreateFlashcardRequest>) {
        apiService.createFlashcard(flashcards)
    }

    suspend fun createExample(example: List<CreateExampleRequest>) {
        apiService.createExamples(example)
    }

    suspend fun createReview(newReviews: List<CreateFlashcardReviewRequest>) {
        apiService.createReviews(newReviews)
    }

    suspend fun createQuote(quoteRequests: List<CreateQuoteRequest>) {
        apiService.createQuotes(quoteRequests)
    }

    suspend fun export() = withContext(Dispatchers.IO) {
        try {
            if (dataStore.firstInitializer) return@withContext
            val inputStream: InputStream = apiService.export().byteStream()
            ZipInputStream(BufferedInputStream(inputStream)).use { zipInput -> zipInput.processAllEntries() }
            dataStore.firstInitializer = true
        } catch (e: Exception) {
            Log.e("TAG", "ZIPINPUT: ${e.message}")
        }
    }

    private suspend fun ZipInputStream.processAllEntries() {
        var entry: ZipEntry? = this.nextEntry
        while (entry != null) {
            if (entry.isDirectory.not()) {
                val typeFile: TypeFile = TypeFile.fromFileName(entry.name) ?: continue
                processNdjsonStream2(this, typeFile)
            }

            this.closeEntry()
            entry = this.nextEntry
        }
    }

    private suspend fun processNdjsonStream2(zipInput: ZipInputStream, typeFile: TypeFile) {
        val reader = BufferedReader(InputStreamReader(zipInput, Charsets.UTF_8))

        var line: String? = reader.readLine()
        while (line != null) {
            if (line.isNotBlank()) {
                try {
                    val item: Any = processJson(line, typeFile) ?: continue
                    processor.process(item)
                } catch (e: Exception) {
                    Log.e("TAG", "BufferedReader: ${e.message}")
                }
            }
            line = reader.readLine()
        }
    }

    private suspend fun processJson(line: String, typeFile: TypeFile): Any? = withContext(Dispatchers.Default) {
        val decodeFromString: Any? = json.decodeFromString(typeFile.serializer, line)
        return@withContext decodeFromString
    }
}
