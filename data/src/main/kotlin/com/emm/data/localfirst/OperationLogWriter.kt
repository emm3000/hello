package com.emm.data.localfirst

import com.emm.data.HelloDb
import com.emm.domain.sync.OperationType
import java.time.Instant
import java.util.UUID

class OperationLogWriter(
    private val db: HelloDb,
) {

    private val localFirstQueries = db.localFirstQueries

    fun appendOperation(
        entityType: String,
        entityId: String,
        operationType: OperationType,
        payloadJson: String,
        originDeviceId: String,
        createdAt: Long = Instant.now().toEpochMilli(),
    ): Long {
        localFirstQueries.incrementLamport(updatedAt = createdAt)
        val lamport = localFirstQueries.currentLamport().executeAsOne()
        localFirstQueries.insertOperation(
            opId = UUID.randomUUID().toString(),
            entityType = entityType,
            entityId = entityId,
            operationType = operationType.name,
            payload = payloadJson,
            lamport = lamport,
            originDeviceId = originDeviceId,
            createdAt = createdAt,
            status = "Pending",
            retryCount = 0L,
            lastAttemptAt = null,
            lastError = null,
        )
        return lamport
    }
}
