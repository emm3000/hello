package com.emm.data.sync

import com.emm.data.HelloDb
import com.emm.data.localfirst.requireCurrentAppAccountId
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.time.Instant

class DrainOutbox(
    private val db: HelloDb,
    private val remote: SupabaseSyncRemoteDataSource,
) {

    private val localFirstQueries = db.localFirstQueries

    suspend operator fun invoke(batchSize: Long = DEFAULT_PUSH_BATCH_SIZE): DrainOutboxResult {
        val appAccountId = db.requireCurrentAppAccountId()
        var totalAcked = 0
        var totalDead = 0
        var totalUnchanged = 0
        var batchesProcessed = 0

        while (batchesProcessed < MAX_BATCHES_PER_SYNC) {
            currentCoroutineContext().ensureActive()

            val pending = localFirstQueries.pendingOperations(
                appAccountId = appAccountId,
                maxRetries = MAX_RETRY_COUNT,
                limit = batchSize,
            ).executeAsList()
            if (pending.isEmpty()) break

            batchesProcessed++
            val counts = pushBatch(pending)
            totalAcked += counts.acked
            totalDead += counts.dead
            totalUnchanged += counts.unchanged
        }

        return if (batchesProcessed == 0) {
            DrainOutboxResult.Empty
        } else {
            DrainOutboxResult.Processed(
                ackedCount = totalAcked,
                deadCount = totalDead,
                unchangedCount = totalUnchanged,
                truncated = batchesProcessed == MAX_BATCHES_PER_SYNC,
            )
        }
    }

    private suspend fun pushBatch(pending: List<com.emm.data.OperationLog>): BatchCounts {
        val now = Instant.now().toEpochMilli()
        var acked = 0
        var dead = 0
        var unchanged = 0

        runCatching {
            val response = remote.push(pending)
            val accepted = response.acceptedOpIds.toSet()
            val rejected = response.rejected.associateBy({ it.opId }, { it.reason })

            pending.forEach { operation ->
                when {
                    accepted.contains(operation.opId) -> {
                        localFirstQueries.markOperationAcked(lastAttemptAt = now, opId = operation.opId)
                        acked += 1
                    }
                    rejected.containsKey(operation.opId) -> {
                        localFirstQueries.markOperationDead(
                            lastAttemptAt = now,
                            lastError = rejected.getValue(operation.opId),
                            opId = operation.opId,
                        )
                        dead += 1
                    }
                    else -> {
                        // Defensive fallback: every pushed op should be either accepted or rejected by the server.
                        // If an op is omitted from both lists, treat it as a failed attempt so retries/backoff progress.
                        localFirstQueries.markOperationFailed(
                            lastAttemptAt = now,
                            lastError = "push_response_missing_status",
                            opId = operation.opId,
                        )
                        unchanged += 1
                    }
                }
            }
        }.getOrElse { error ->
            pending.forEach { operation ->
                localFirstQueries.markOperationFailed(
                    lastAttemptAt = now,
                    lastError = error.message ?: "push_failed",
                    opId = operation.opId,
                )
            }
            throw error
        }

        return BatchCounts(acked = acked, dead = dead, unchanged = unchanged)
    }

    companion object {
        const val DEFAULT_PUSH_BATCH_SIZE = 100L
        const val MAX_RETRY_COUNT = 5L

        // Caps the number of batches per sync cycle to avoid WorkManager timeouts (~10 min limit).
        // At 100 ops/batch this allows up to 2000 ops per cycle; remaining ops are picked up next sync.
        const val MAX_BATCHES_PER_SYNC = 20
    }
}

private data class BatchCounts(val acked: Int, val dead: Int, val unchanged: Int)

sealed interface DrainOutboxResult {
    data object Empty : DrainOutboxResult

    data class Processed(
        val ackedCount: Int,
        val deadCount: Int,
        val unchangedCount: Int,
        // true if the batch limit was reached — caller should schedule another sync cycle
        val truncated: Boolean = false,
    ) : DrainOutboxResult
}
