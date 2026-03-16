package com.emm.data.sync

import com.emm.data.HelloDb
import java.time.Instant

class DrainOutbox(
    private val db: HelloDb,
    private val remote: SupabaseSyncRemoteDataSource,
) {

    private val localFirstQueries = db.localFirstQueries

    suspend operator fun invoke(batchSize: Long = DEFAULT_PUSH_BATCH_SIZE): DrainOutboxResult {
        val pending = localFirstQueries.pendingOperations(batchSize).executeAsList()
        if (pending.isEmpty()) return DrainOutboxResult.Empty

        val now = Instant.now().toEpochMilli()
        return runCatching {
            val response = remote.push(pending)
            val accepted = response.acceptedOpIds.toSet()
            val rejected = response.rejected.associateBy({ it.opId }, { it.reason })
            var ackedCount = 0
            var deadCount = 0
            var unchangedCount = 0

            pending.forEach { operation ->
                when {
                    accepted.contains(operation.opId) -> {
                        localFirstQueries.markOperationAcked(
                            lastAttemptAt = now,
                            opId = operation.opId,
                        )
                        ackedCount += 1
                    }
                    rejected.containsKey(operation.opId) -> {
                        localFirstQueries.markOperationDead(
                            lastAttemptAt = now,
                            lastError = rejected.getValue(operation.opId),
                            opId = operation.opId,
                        )
                        deadCount += 1
                    }
                    else -> {
                        unchangedCount += 1
                    }
                }
            }

            DrainOutboxResult.Processed(
                ackedCount = ackedCount,
                deadCount = deadCount,
                unchangedCount = unchangedCount,
            )
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
    }

    companion object {
        const val DEFAULT_PUSH_BATCH_SIZE = 100L
    }
}

sealed interface DrainOutboxResult {
    data object Empty : DrainOutboxResult

    data class Processed(
        val ackedCount: Int,
        val deadCount: Int,
        val unchangedCount: Int,
    ) : DrainOutboxResult
}
