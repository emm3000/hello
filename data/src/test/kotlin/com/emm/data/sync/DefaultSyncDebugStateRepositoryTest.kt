package com.emm.data.sync

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.HelloDb
import com.emm.domain.sync.SyncDebugState
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

class DefaultSyncDebugStateRepositoryTest {
    private lateinit var db: HelloDb
    private lateinit var subject: DefaultSyncDebugStateRepository

    @Before
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        HelloDb.Schema.create(driver)
        db = HelloDb(driver)
        subject = DefaultSyncDebugStateRepository(db)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `observe emits updates when checkpoint and pending operations change`() = runTest {
        seedLocalDeviceIdentity()
        seedLocalAccount(accountId = "account-1")

        val emissions = LinkedBlockingQueue<SyncDebugState>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            subject.observe().collect { emissions.put(it) }
        }

        val initial = emissions.awaitState { state ->
            state.appAccountId == "account-1" &&
                state.deviceId == "device-1" &&
                state.pendingOperations == 0L &&
                state.lastSuccessfulSyncAt == null &&
                state.lastSyncError == null
        }
        assertEquals("account-1", initial.appAccountId)

        db.localFirstQueries.upsertSyncCheckpoint(
            appAccountId = "account-1",
            lastPulledCursor = 7L,
            lastSuccessfulSyncAt = 1234L,
            lastSyncError = "network timeout",
            lastSyncErrorAt = 1235L,
            updatedAt = 1236L,
        )

        val checkpointState = emissions.awaitState { state ->
            state.lastSuccessfulSyncAt == 1234L && state.lastSyncError == "network timeout"
        }
        assertEquals(1234L, checkpointState.lastSuccessfulSyncAt)

        insertPendingOperation(accountId = "account-1", opId = "pending-op-1")

        val pendingState = emissions.awaitState { state -> state.pendingOperations == 1L }
        assertEquals(1L, pendingState.pendingOperations)

        job.cancel()
    }

    private fun seedLocalDeviceIdentity() {
        db.localFirstQueries.upsertLocalDeviceIdentity(
            deviceId = "device-1",
            installId = "install-1",
            lamportCounter = 0L,
            createdAt = 1L,
            updatedAt = 1L,
        )
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

private fun LinkedBlockingQueue<SyncDebugState>.awaitState(
    predicate: (SyncDebugState) -> Boolean,
): SyncDebugState {
    while (true) {
        val item = poll(5, TimeUnit.SECONDS) ?: error("Timed out waiting for flow emission")
        if (predicate(item)) return item
    }
}
