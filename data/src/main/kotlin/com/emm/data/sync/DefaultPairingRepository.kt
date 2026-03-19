package com.emm.data.sync

import android.os.Build
import com.emm.data.HelloDb
import com.emm.data.localfirst.LocalDeviceIdentityProvider
import com.emm.data.remote.DataStore
import com.emm.domain.sync.LinkedDevice
import com.emm.domain.sync.PairingRepository
import com.emm.domain.sync.PairingSession
import java.time.Instant

class DefaultPairingRepository(
    private val remote: SupabaseSyncRemoteDataSource,
    private val localDeviceIdentityProvider: LocalDeviceIdentityProvider,
    private val db: HelloDb,
    private val dataStore: DataStore,
) : PairingRepository {

    override suspend fun ensureLinkedIdentity() {
        val deviceId = localDeviceIdentityProvider.getOrCreateDeviceId()
        remote.ensureAnonymousSession()
        val bootstrap = remote.bootstrapAnonymousDevice(
            deviceId = deviceId,
            deviceName = Build.MODEL,
            platform = "android",
        )
        persistLocalAccountState(
            appAccountId = bootstrap.appAccountId,
            authUserId = bootstrap.authUserId,
            resetSyncCursor = false,
        )
    }

    override suspend fun createPairingSession(ttlMinutes: Int): PairingSession {
        val session = remote.createPairingSession(ttlMinutes = ttlMinutes)
        return PairingSession(
            code = session.code,
            expiresAt = session.expiresAt,
        )
    }

    override suspend fun redeemPairingCode(code: String) {
        val deviceId = localDeviceIdentityProvider.getOrCreateDeviceId()
        remote.ensureAnonymousSession()
        val redeem = remote.redeemPairingCode(
            code = code,
            deviceId = deviceId,
            deviceName = Build.MODEL,
            platform = "android",
        )
        persistLocalAccountState(
            appAccountId = redeem.appAccountId,
            authUserId = redeem.authUserId,
            resetSyncCursor = true,
        )
    }

    override suspend fun listLinkedDevices(): List<LinkedDevice> {
        return remote.listLinkedDevices().map { device ->
            LinkedDevice(
                id = device.id,
                deviceName = device.deviceName,
                platform = device.platform,
                createdAt = device.createdAt,
                lastSeenAt = device.lastSeenAt,
                revokedAt = device.revokedAt,
                isCurrent = device.isCurrent,
            )
        }
    }

    override suspend fun revokeLinkedDevice(deviceId: String, reason: String?): Boolean {
        return remote.revokeLinkedDevice(deviceId = deviceId, reason = reason)
    }

    private fun persistLocalAccountState(
        appAccountId: String,
        authUserId: String,
        resetSyncCursor: Boolean,
    ) {
        val queries = db.localFirstQueries
        val now = Instant.now().toEpochMilli()
        val current = queries.selectLocalAccountState().executeAsOneOrNull()
        val accountChanged = resetSyncCursor &&
            current?.appAccountId != null &&
            current.appAccountId != appAccountId

        db.transaction {
            if (accountChanged) {
                clearAccountScopedLocalState()
            }

            queries.upsertLocalAccountState(
                appAccountId = appAccountId,
                authUserId = authUserId,
                pairingState = "Paired",
                createdAt = current?.createdAt ?: now,
                updatedAt = now,
            )

            if (resetSyncCursor) {
                val checkpoint = queries.selectSyncCheckpoint().executeAsOneOrNull()
                queries.resetSyncCheckpoint()
                queries.upsertSyncCheckpoint(
                    lastPulledCursor = 0L,
                    lastSuccessfulSyncAt = checkpoint?.lastSuccessfulSyncAt,
                    lastSyncError = null,
                    lastSyncErrorAt = null,
                    updatedAt = now,
                )
            }
        }

        if (accountChanged) {
            dataStore.clearDefaultDeck()
        }
    }

    private fun clearAccountScopedLocalState() {
        db.localFirstQueries.deleteAllReviewProjections()
        db.localFirstQueries.deleteAllReviewEvents()
        db.flashcardExampleQueries.deleteAllFlashcardExamples()
        db.flashcardQueries.deleteAllFlashcards()
        db.deckQueries.deleteAllDecks()
        db.quotesQueries.deleteAllQuotes()
        db.localFirstQueries.deleteAllOperations()
        db.localFirstQueries.deleteAllAppliedRemoteOperations()
    }
}
