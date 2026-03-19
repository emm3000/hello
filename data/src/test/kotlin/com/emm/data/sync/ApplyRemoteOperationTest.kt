package com.emm.data.sync

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.HelloDb
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ApplyRemoteOperationTest {

    private lateinit var db: HelloDb
    private lateinit var subject: ApplyRemoteOperation

    @Before
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        HelloDb.Schema.create(driver)
        db = HelloDb(driver)
        subject = ApplyRemoteOperation(db)
    }

    @Test
    fun `apply deck create inserts row and returns applied`() {
        val result = subject(
            operation = operation(
                entityType = "deck",
                entityId = "deck-1",
                operationType = "create",
                lamport = 10,
                payload = buildJsonObject {
                    put("name", "Travel")
                    put("description", "Trip deck")
                    put("createdAt", 1000)
                    put("updatedAt", 1000)
                }
            ),
            localDeviceId = "local-device"
        )

        assertTrue(result is ApplyRemoteOperationResult.Applied)
        val row = db.deckQueries.findById("deck-1").executeAsOneOrNull()
        assertEquals("Travel", row?.name)
        assertEquals(10L, row?.versionLamport)
    }

    @Test
    fun `stale deck operation is skipped and does not overwrite current row`() {
        subject(
            operation = operation(
                entityType = "deck",
                entityId = "deck-1",
                operationType = "create",
                lamport = 20,
                payload = buildJsonObject {
                    put("name", "Fresh")
                    put("createdAt", 1000)
                    put("updatedAt", 1000)
                }
            ),
            localDeviceId = "local-device"
        )

        val staleResult = subject(
            operation = operation(
                entityType = "deck",
                entityId = "deck-1",
                operationType = "update",
                lamport = 10,
                payload = buildJsonObject {
                    put("name", "Stale")
                    put("updatedAt", 2000)
                }
            ),
            localDeviceId = "local-device"
        )

        assertEquals(
            ApplyRemoteOperationResult.Skipped(reason = "stale_lamport"),
            staleResult
        )
        val row = db.deckQueries.findById("deck-1").executeAsOneOrNull()
        assertEquals("Fresh", row?.name)
        assertEquals(20L, row?.versionLamport)
    }

    @Test
    fun `equal lamport with lower device id is skipped`() {
        subject(
            operation = operation(
                entityType = "deck",
                entityId = "deck-1",
                operationType = "create",
                lamport = 20,
                originDeviceId = "device-b",
                payload = buildJsonObject {
                    put("name", "Winner")
                    put("createdAt", 1000)
                    put("updatedAt", 1000)
                }
            ),
            localDeviceId = "local-device"
        )

        val staleResult = subject(
            operation = operation(
                entityType = "deck",
                entityId = "deck-1",
                operationType = "update",
                lamport = 20,
                originDeviceId = "device-a",
                payload = buildJsonObject {
                    put("name", "Loser")
                    put("updatedAt", 2000)
                }
            ),
            localDeviceId = "local-device"
        )

        assertEquals(
            ApplyRemoteOperationResult.Skipped(reason = "stale_lamport"),
            staleResult
        )
        val row = db.deckQueries.findById("deck-1").executeAsOneOrNull()
        assertEquals("Winner", row?.name)
        assertEquals("device-b", row?.lastModifiedByDeviceId)
    }

    @Test
    fun `equal lamport with higher device id overwrites existing row`() {
        subject(
            operation = operation(
                entityType = "deck",
                entityId = "deck-1",
                operationType = "create",
                lamport = 20,
                originDeviceId = "device-a",
                payload = buildJsonObject {
                    put("name", "First")
                    put("createdAt", 1000)
                    put("updatedAt", 1000)
                }
            ),
            localDeviceId = "local-device"
        )

        val result = subject(
            operation = operation(
                entityType = "deck",
                entityId = "deck-1",
                operationType = "update",
                lamport = 20,
                originDeviceId = "device-b",
                payload = buildJsonObject {
                    put("name", "Second")
                    put("updatedAt", 2000)
                }
            ),
            localDeviceId = "local-device"
        )

        assertTrue(result is ApplyRemoteOperationResult.Applied)
        val row = db.deckQueries.findById("deck-1").executeAsOneOrNull()
        assertEquals("Second", row?.name)
        assertEquals("device-b", row?.lastModifiedByDeviceId)
    }

    @Test
    fun `flashcard create without parent deck is deferred`() {
        val result = subject(
            operation = operation(
                entityType = "flashcard",
                entityId = "card-1",
                operationType = "create",
                lamport = 1,
                payload = buildJsonObject {
                    put("deckId", "missing-deck")
                    put("word", "book")
                    put("meaning", "a written work")
                }
            ),
            localDeviceId = "local-device"
        )

        assertEquals(
            ApplyRemoteOperationResult.Deferred(reason = "missing_parent_deck"),
            result
        )
    }

    @Test
    fun `review event with missing fields is deferred`() {
        val result = subject(
            operation = operation(
                entityType = "review_event",
                entityId = "event-1",
                operationType = "create",
                lamport = 5,
                payload = buildJsonObject {
                    put("flashcardId", "card-1")
                }
            ),
            localDeviceId = "local-device"
        )

        assertEquals(
            ApplyRemoteOperationResult.Deferred(reason = "missing_required_field:reviewed_at"),
            result
        )
    }

    @Test
    fun `review event creates projection when payload is valid`() {
        subject(
            operation = operation(
                entityType = "deck",
                entityId = "deck-1",
                operationType = "create",
                lamport = 1,
                payload = buildJsonObject {
                    put("name", "Deck")
                    put("createdAt", 1000)
                    put("updatedAt", 1000)
                }
            ),
            localDeviceId = "local-device"
        )

        subject(
            operation = operation(
                entityType = "flashcard",
                entityId = "card-1",
                operationType = "create",
                lamport = 2,
                payload = buildJsonObject {
                    put("deckId", "deck-1")
                    put("word", "book")
                    put("meaning", "a written work")
                    put("createdAt", 1000)
                    put("updatedAt", 1000)
                }
            ),
            localDeviceId = "local-device"
        )

        val result = subject(
            operation = operation(
                entityType = "review_event",
                entityId = "event-1",
                operationType = "create",
                lamport = 3,
                payload = buildJsonObject {
                    put("flashcardId", "card-1")
                    put("reviewedAt", 2000)
                    put("nextReviewAt", 3000)
                    put("easeFactor", 2.5)
                    put("interval", 2)
                    put("repetitions", 1)
                    put("lapses", 0)
                    put("createdAt", 2000)
                }
            ),
            localDeviceId = "local-device"
        )

        assertTrue(result is ApplyRemoteOperationResult.Applied)
        val projection = db.localFirstQueries.findReviewProjectionByFlashcardId("card-1").executeAsOneOrNull()
        assertEquals(2000L, projection?.lastReviewedAt)
        assertEquals(3000L, projection?.nextReviewAt)
    }

    private fun operation(
        entityType: String,
        entityId: String,
        operationType: String,
        lamport: Long,
        originDeviceId: String = "remote-device",
        payload: kotlinx.serialization.json.JsonObject,
    ): RemoteSyncOperation {
        return RemoteSyncOperation(
            cursor = lamport,
            opId = "op-$lamport-$entityType-$entityId",
            entityType = entityType,
            entityId = entityId,
            operationType = operationType,
            payload = payload,
            lamport = lamport,
            originDeviceId = originDeviceId,
            createdAt = "2026-03-16T10:00:00Z",
        )
    }
}
