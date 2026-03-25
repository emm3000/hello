package com.emm.data.sync

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.HelloDb
import com.emm.data.flashcard.DefaultFlashcardRepository
import com.emm.data.flashcard.GeminiService
import com.emm.data.localfirst.LocalDeviceIdentityProvider
import com.emm.data.localfirst.OperationLogWriter
import com.emm.domain.flashcard.CreateFlashcardInput
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
    private val accountId = "account-1"

    private lateinit var db: HelloDb
    private lateinit var json: Json
    private lateinit var operationLogWriter: OperationLogWriter
    private lateinit var localDeviceIdentityProvider: LocalDeviceIdentityProvider

    @Before
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        HelloDb.Schema.create(driver)
        db = HelloDb(driver)
        db.localFirstQueries.upsertLocalAccountState(
            appAccountId = accountId,
            authUserId = "auth-1",
            pairingState = "Paired",
            createdAt = 1,
            updatedAt = 1,
        )
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
            .pendingOperations(accountId, DrainOutbox.MAX_RETRY_COUNT, 1)
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

    private fun seedDeck(id: String) {
        db.deckQueries.insert(
            appAccountId = accountId,
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
