package com.emm.data.sync

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.HelloDb
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RetryableOperationsCountTest {

    private lateinit var db: HelloDb

    @Before
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        HelloDb.Schema.create(driver)
        db = HelloDb(driver)
    }

    @Test
    fun `countRetryableOperations includes pending and failed below retry limit`() {
        db.localFirstQueries.insertOperation(
            opId = "pending-op",
            entityType = "deck",
            entityId = "deck-1",
            operationType = "Create",
            payload = "{}",
            lamport = 1,
            originDeviceId = "device-a",
            createdAt = 1,
            status = "Pending",
            retryCount = 0,
            lastAttemptAt = null,
            lastError = null,
        )
        db.localFirstQueries.insertOperation(
            opId = "failed-op",
            entityType = "deck",
            entityId = "deck-2",
            operationType = "Create",
            payload = "{}",
            lamport = 2,
            originDeviceId = "device-a",
            createdAt = 2,
            status = "Failed",
            retryCount = 4,
            lastAttemptAt = 2,
            lastError = "timeout",
        )
        db.localFirstQueries.insertOperation(
            opId = "dead-op",
            entityType = "deck",
            entityId = "deck-3",
            operationType = "Create",
            payload = "{}",
            lamport = 3,
            originDeviceId = "device-a",
            createdAt = 3,
            status = "Failed",
            retryCount = 5,
            lastAttemptAt = 3,
            lastError = "too_many_retries",
        )

        val count = db.localFirstQueries
            .countRetryableOperations(maxRetries = DrainOutbox.MAX_RETRY_COUNT)
            .executeAsOne()

        assertEquals(2L, count)
    }

    @Test
    fun `pendingOperations are drained in lamport order`() {
        db.localFirstQueries.insertOperation(
            opId = "op-lamport-2",
            entityType = "deck",
            entityId = "deck-2",
            operationType = "Create",
            payload = "{}",
            lamport = 2,
            originDeviceId = "device-a",
            createdAt = 1,
            status = "Pending",
            retryCount = 0,
            lastAttemptAt = null,
            lastError = null,
        )
        db.localFirstQueries.insertOperation(
            opId = "op-lamport-1",
            entityType = "deck",
            entityId = "deck-1",
            operationType = "Create",
            payload = "{}",
            lamport = 1,
            originDeviceId = "device-a",
            createdAt = 2,
            status = "Pending",
            retryCount = 0,
            lastAttemptAt = null,
            lastError = null,
        )

        val pending = db.localFirstQueries
            .pendingOperations(maxRetries = DrainOutbox.MAX_RETRY_COUNT, limit = 10)
            .executeAsList()

        assertEquals(listOf("op-lamport-1", "op-lamport-2"), pending.map { it.opId })
    }
}
