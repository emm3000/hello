package com.emm.data.sync

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.HelloDb
import com.emm.data.localfirst.LocalDeviceIdentityProvider
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class DefaultSyncEngineTest {
    private val accountId = "account-1"

    private lateinit var db: HelloDb
    private lateinit var remote: SupabaseSyncRemoteDataSource
    private lateinit var drainOutbox: DrainOutbox
    private lateinit var pullRemoteOperations: PullRemoteOperations
    private lateinit var applyRemoteOperation: ApplyRemoteOperation
    private lateinit var ackOperations: AckOperations
    private lateinit var subject: DefaultSyncEngine

    @Before
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        HelloDb.Schema.create(driver)
        db = HelloDb(driver)
        remote = mockk()
        drainOutbox = mockk()
        pullRemoteOperations = mockk()
        applyRemoteOperation = mockk()
        ackOperations = mockk()

        val identityProvider = LocalDeviceIdentityProvider(db)
        subject = DefaultSyncEngine(
            db = db,
            remote = remote,
            localDeviceIdentityProvider = identityProvider,
            drainOutbox = drainOutbox,
            pullRemoteOperations = pullRemoteOperations,
            applyRemoteOperation = applyRemoteOperation,
            ackOperations = ackOperations,
        )
    }

    @Test
    fun `successful sync persists account state and checkpoint`() = runTest {
        stubBootstrap()
        coEvery { drainOutbox.invoke(any()) } returns DrainOutboxResult.Empty
        coEvery { pullRemoteOperations.invoke(any()) } returns PullRemoteOperationsResult(
            currentCursor = 42,
            operations = emptyList(),
        )

        subject.runOnce()

        val checkpoint = db.localFirstQueries.selectSyncCheckpoint(accountId).executeAsOneOrNull()
        val accountState = db.localFirstQueries.selectLocalAccountState().executeAsOneOrNull()
        assertEquals(42L, checkpoint?.lastPulledCursor)
        assertEquals("account-1", accountState?.appAccountId)
        assertNotNull(subject.state.value.lastSuccessfulSyncAt)
        assertEquals(0L, subject.state.value.pendingOperations)
    }

    @Test
    fun `sync failure stores last error without losing previous cursor`() = runTest {
        stubBootstrap()
        db.localFirstQueries.upsertSyncCheckpoint(
            appAccountId = accountId,
            lastPulledCursor = 7,
            lastSuccessfulSyncAt = 100,
            lastSyncError = null,
            lastSyncErrorAt = null,
            updatedAt = 100,
        )
        coEvery { drainOutbox.invoke(any()) } throws IOException("network timeout")

        val result = runCatching { subject.runOnce() }

        assertTrue(result.isFailure)
        val checkpoint = db.localFirstQueries.selectSyncCheckpoint(accountId).executeAsOneOrNull()
        assertEquals(7L, checkpoint?.lastPulledCursor)
        assertEquals("network timeout", checkpoint?.lastSyncError)
        assertEquals("network timeout", subject.state.value.lastSyncError)
    }

    @Test
    fun `deferred operation caps checkpoint before deferred cursor`() = runTest {
        stubBootstrap()
        coEvery { drainOutbox.invoke(any()) } returns DrainOutboxResult.Empty
        val deferred = remoteOperation(opId = "op-5", cursor = 5, entityId = "deck-5")
        val applied = remoteOperation(opId = "op-6", cursor = 6, entityId = "deck-6")
        coEvery { pullRemoteOperations.invoke(any()) } returns PullRemoteOperationsResult(
            currentCursor = 0,
            operations = listOf(deferred, applied),
        )
        coEvery {
            applyRemoteOperation.invoke(deferred, any())
        } returns ApplyRemoteOperationResult.Deferred("missing_parent")
        coEvery { applyRemoteOperation.invoke(applied, any()) } returns ApplyRemoteOperationResult.Applied
        coEvery { ackOperations.invoke(listOf("op-6")) } returns 1

        subject.runOnce()

        val checkpoint = db.localFirstQueries.selectSyncCheckpoint(accountId).executeAsOneOrNull()
        assertEquals(4L, checkpoint?.lastPulledCursor)
    }

    @Test
    fun `already processed remote operation is acked without reapplying`() = runTest {
        stubBootstrap()
        db.localFirstQueries.markRemoteOperationProcessed(
            appAccountId = accountId,
            opId = "op-9",
            cursor = 9,
            entityType = "deck",
            entityId = "deck-9",
            operationType = "create",
            processedAt = 1,
        )
        coEvery { drainOutbox.invoke(any()) } returns DrainOutboxResult.Empty
        coEvery { pullRemoteOperations.invoke(any()) } returnsMany listOf(
            PullRemoteOperationsResult(
                currentCursor = 9,
                operations = listOf(remoteOperation(opId = "op-9", cursor = 9, entityId = "deck-9")),
            ),
            PullRemoteOperationsResult(
                currentCursor = 9,
                operations = emptyList(),
            ),
        )
        coEvery { applyRemoteOperation.invoke(any(), any()) } answers {
            throw AssertionError("Processed remote ops must not be reapplied")
        }
        coEvery { ackOperations.invoke(listOf("op-9")) } returns 1

        subject.runOnce()

        val checkpoint = db.localFirstQueries.selectSyncCheckpoint(accountId).executeAsOneOrNull()
        assertEquals(9L, checkpoint?.lastPulledCursor)
    }

    private fun stubBootstrap() {
        coEvery { remote.ensureAnonymousSession() } returns Unit
        coEvery { remote.bootstrapAnonymousDevice(any(), any(), any()) } returns SyncBootstrapResponse(
            appAccountId = accountId,
            appDeviceId = "device-1",
            authUserId = "auth-1",
            created = false,
        )
    }

    private fun remoteOperation(
        opId: String,
        cursor: Long,
        entityId: String = "deck-1",
    ) = RemoteSyncOperation(
        cursor = cursor,
        opId = opId,
        entityType = "deck",
        entityId = entityId,
        operationType = "create",
        payload = buildJsonObject {
            put("name", "Deck")
            put("createdAt", 1000)
            put("updatedAt", 1000)
        },
        lamport = cursor,
        originDeviceId = "remote-device",
        createdAt = "2026-03-16T10:00:00Z",
    )
}
