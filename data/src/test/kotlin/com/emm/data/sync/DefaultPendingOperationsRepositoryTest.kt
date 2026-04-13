package com.emm.data.sync

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.HelloDb
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class DefaultPendingOperationsRepositoryTest {
    private lateinit var db: HelloDb
    private lateinit var subject: DefaultPendingOperationsRepository

    @Before
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        HelloDb.Schema.create(driver)
        db = HelloDb(driver)
        subject = DefaultPendingOperationsRepository(db)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `observeHasPendingOperations switches to the latest active account`() = runTest {
        seedLocalAccount(accountId = "account-old")
        insertPendingOperation(accountId = "account-old", opId = "old-op")

        val emissions = LinkedBlockingQueue<Boolean>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            subject.observeHasPendingOperations().collect { emissions.put(it) }
        }

        assertEquals(true, emissions.awaitItem())

        seedLocalAccount(accountId = "account-new")
        assertEquals(false, emissions.awaitItem())

        insertPendingOperation(accountId = "account-new", opId = "new-op")
        assertEquals(true, emissions.awaitItem())

        job.cancel()
    }

    private fun seedLocalAccount(accountId: String) {
        db.localFirstQueries.upsertLocalAccountState(
            appAccountId = accountId,
            authUserId = "auth-$accountId",
            pairingState = "Paired",
            createdAt = 1L,
            updatedAt = 1L,
        )
    }

    private fun insertPendingOperation(accountId: String, opId: String) {
        db.localFirstQueries.insertOperation(
            opId = opId,
            appAccountId = accountId,
            entityType = "deck",
            entityId = "deck-$opId",
            operationType = "Create",
            payload = "{}",
            lamport = 1L,
            originDeviceId = "device-1",
            createdAt = 1L,
            status = "Pending",
            retryCount = 0L,
            lastAttemptAt = null,
            lastError = null,
        )
    }
}

private fun <T> LinkedBlockingQueue<T>.awaitItem(): T {
    return poll(5, TimeUnit.SECONDS) ?: error("Timed out waiting for flow emission")
}
