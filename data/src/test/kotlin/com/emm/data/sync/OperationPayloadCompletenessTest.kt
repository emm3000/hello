package com.emm.data.sync

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.HelloDb
import com.emm.data.flashcard.DefaultFlashcardRepository
import com.emm.data.flashcard.GeminiService
import com.emm.data.localfirst.LocalDeviceIdentityProvider
import com.emm.data.localfirst.OperationLogWriter
import com.emm.data.quote.DefaultQuoteRepository
import com.emm.domain.flashcard.CreateFlashcardInput
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class OperationPayloadCompletenessTest {

    private lateinit var db: HelloDb
    private lateinit var json: Json
    private lateinit var operationLogWriter: OperationLogWriter
    private lateinit var localDeviceIdentityProvider: LocalDeviceIdentityProvider

    @Before
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        HelloDb.Schema.create(driver)
        db = HelloDb(driver)
        json = Json
        operationLogWriter = OperationLogWriter(db)
        localDeviceIdentityProvider = LocalDeviceIdentityProvider(db)
    }

    @Test
    fun `flashcard operation payload includes full entity snapshot`() = runTest {
        seedDeck(id = "deck-1")
        val repo = DefaultFlashcardRepository(
            db = db,
            geminiService = mockk<GeminiService>(),
            json = json,
            operationLogWriter = operationLogWriter,
            localDeviceIdentityProvider = localDeviceIdentityProvider,
        )

        repo.create(
            CreateFlashcardInput(
                deckId = "deck-1",
                word = "book",
                meaning = "a written work",
                translation = "libro",
                phonetic = "/bʊk/",
                partOfSpeech = "noun",
                type = "word",
                note = "common noun",
            )
        )

        val operation = db.localFirstQueries
            .pendingOperations(maxRetries = DrainOutbox.MAX_RETRY_COUNT, limit = 1)
            .executeAsOne()
        val payload = json.parseToJsonElement(operation.payload).jsonObject

        assertEquals("deck-1", payload["deckId"]?.jsonPrimitive?.content)
        assertEquals("book", payload["word"]?.jsonPrimitive?.content)
        assertEquals("a written work", payload["meaning"]?.jsonPrimitive?.content)
        assertEquals("libro", payload["translation"]?.jsonPrimitive?.content)
        assertEquals("/bʊk/", payload["phonetic"]?.jsonPrimitive?.content)
        assertEquals("noun", payload["partOfSpeech"]?.jsonPrimitive?.content)
        assertEquals("word", payload["type"]?.jsonPrimitive?.content)
        assertEquals("common noun", payload["note"]?.jsonPrimitive?.content)
        assertNotNull(payload["createdAt"])
        assertNotNull(payload["updatedAt"])
    }

    @Test
    fun `quote operation payload includes full entity snapshot`() = runTest {
        val gemini = mockk<GeminiService>()
        coEvery {
            gemini.process(any())
        } returns """
            {
              "success": true,
              "data": {
                "category": "travel",
                "context": "airport",
                "description": "useful phrase",
                "example": "I need a taxi",
                "formality": "neutral",
                "phrase": "I need a taxi",
                "pronunciation": "ai niid a taksi",
                "tags": ["transport", "basic"],
                "title": "Taxi phrase",
                "translation": "Necesito un taxi"
              }
            }
        """.trimIndent()
        val repo = DefaultQuoteRepository(
            db = db,
            geminiApi = gemini,
            json = json,
            operationLogWriter = operationLogWriter,
            localDeviceIdentityProvider = localDeviceIdentityProvider,
        )

        repo.generate()

        val operation = db.localFirstQueries
            .pendingOperations(maxRetries = DrainOutbox.MAX_RETRY_COUNT, limit = 1)
            .executeAsOne()
        val payload = json.parseToJsonElement(operation.payload).jsonObject

        assertEquals("Taxi phrase", payload["title"]?.jsonPrimitive?.content)
        assertEquals("I need a taxi", payload["phrase"]?.jsonPrimitive?.content)
        assertEquals("useful phrase", payload["description"]?.jsonPrimitive?.content)
        assertEquals("Necesito un taxi", payload["translation"]?.jsonPrimitive?.content)
        assertEquals("I need a taxi", payload["example"]?.jsonPrimitive?.content)
        assertEquals("airport", payload["context"]?.jsonPrimitive?.content)
        assertEquals("ai niid a taksi", payload["pronunciation"]?.jsonPrimitive?.content)
        assertEquals("neutral", payload["formality"]?.jsonPrimitive?.content)
        assertEquals("transport|basic", payload["tags"]?.jsonPrimitive?.content)
        assertEquals("travel", payload["category"]?.jsonPrimitive?.content)
        assertNotNull(payload["createdAt"])
        assertNotNull(payload["updatedAt"])
    }

    private fun seedDeck(id: String) {
        db.deckQueries.insert(
            id = id,
            name = "Deck",
            description = null,
            createdAt = 1L,
            updatedAt = 1L,
            deletedAt = null,
            originDeviceId = "device-1",
            lastModifiedByDeviceId = "device-1",
            versionLamport = 1L,
        )
    }
}
