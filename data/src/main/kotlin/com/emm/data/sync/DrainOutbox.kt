package com.emm.data.sync

import com.emm.data.HelloDb
import java.time.Instant

class DrainOutbox(
    private val db: HelloDb,
    private val remote: SupabaseSyncRemoteDataSource,
) {

    private val localFirstQueries = db.localFirstQueries

    suspend operator fun invoke(batchSize: Long = DEFAULT_PUSH_BATCH_SIZE): DrainOutboxResult {
        var totalAcked = 0
        var totalDead = 0
        var totalUnchanged = 0
        var anyProcessed = false

        while (true) {
            val pending = localFirstQueries.pendingOperations(
                maxRetries = MAX_RETRY_COUNT,
                limit = batchSize,
            ).executeAsList()
            if (pending.isEmpty()) break
            anyProcessed = true

            val counts = pushBatch(pending)
            totalAcked += counts.acked
            totalDead += counts.dead
            totalUnchanged += counts.unchanged
        }

        return if (!anyProcessed) {
            DrainOutboxResult.Empty
        } else {
            DrainOutboxResult.Processed(
                ackedCount = totalAcked,
                deadCount = totalDead,
                unchangedCount = totalUnchanged,
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
                    else -> unchanged += 1
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
    }
}

private data class BatchCounts(val acked: Int, val dead: Int, val unchanged: Int)

sealed interface DrainOutboxResult {
    data object Empty : DrainOutboxResult

    data class Processed(
        val ackedCount: Int,
        val deadCount: Int,
        val unchangedCount: Int,
    ) : DrainOutboxResult
}
