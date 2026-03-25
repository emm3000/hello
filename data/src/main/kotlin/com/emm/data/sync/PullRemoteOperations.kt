package com.emm.data.sync

import com.emm.data.HelloDb
import com.emm.data.localfirst.requireCurrentAppAccountId

class PullRemoteOperations(
    private val db: HelloDb,
    private val remote: SupabaseSyncRemoteDataSource,
) {

    private val localFirstQueries = db.localFirstQueries

    suspend operator fun invoke(limit: Int = DEFAULT_PULL_BATCH_SIZE): PullRemoteOperationsResult {
        val appAccountId = db.requireCurrentAppAccountId()
        val checkpoint = localFirstQueries.selectSyncCheckpoint(appAccountId).executeAsOneOrNull()
        val currentCursor = checkpoint?.lastPulledCursor ?: 0L
        val operations = remote.pull(cursor = currentCursor, limit = limit)
        return PullRemoteOperationsResult(
            currentCursor = currentCursor,
            operations = operations,
        )
    }

    companion object {
        const val DEFAULT_PULL_BATCH_SIZE = 200
    }
}

data class PullRemoteOperationsResult(
    val currentCursor: Long,
    val operations: List<RemoteSyncOperation>,
)
