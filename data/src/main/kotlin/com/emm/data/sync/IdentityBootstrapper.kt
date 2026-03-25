package com.emm.data.sync

import android.os.Build
import com.emm.data.HelloDb
import com.emm.data.localfirst.LocalDeviceIdentityProvider
import com.emm.data.logging.logInfo
import com.emm.data.remote.DataStore
import java.time.Instant

class IdentityBootstrapper(
    private val remote: SupabaseSyncRemoteDataSource,
    private val localDeviceIdentityProvider: LocalDeviceIdentityProvider,
    private val db: HelloDb,
    private val dataStore: DataStore,
) {

    suspend fun ensureIdentityReady() {
        logInfo(TAG, "ensureIdentityReady:start")
        val deviceId = localDeviceIdentityProvider.getOrCreateDeviceId()
        remote.ensureAnonymousSession()
        logInfo(TAG, "ensureIdentityReady:session_ready deviceId=$deviceId")

        val bootstrap = remote.bootstrapAnonymousDevice(
            deviceId = deviceId,
            deviceName = Build.MODEL,
            platform = "android",
        )
        logInfo(
            TAG,
            "ensureIdentityReady:bootstrap_success deviceId=$deviceId appAccountId=${bootstrap.appAccountId} created=${bootstrap.created}"
        )

        persistLocalAccountState(
            appAccountId = bootstrap.appAccountId,
            authUserId = bootstrap.authUserId,
            resetSyncCursor = false,
        )
        logInfo(TAG, "ensureIdentityReady:local_state_persisted")
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
            queries.upsertLocalAccountState(
                appAccountId = appAccountId,
                authUserId = authUserId,
                pairingState = "Paired",
                createdAt = current?.createdAt ?: now,
                updatedAt = now,
            )

            if (resetSyncCursor) {
                val checkpoint = queries.selectSyncCheckpoint(appAccountId).executeAsOneOrNull()
                queries.upsertSyncCheckpoint(
                    appAccountId = appAccountId,
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
            logInfo(TAG, "persistLocalAccountState:account_changed cleared_default_deck")
        }
    }
}

private const val TAG = "IdentityBootstrap"
