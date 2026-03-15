package com.emm.data.localfirst

import com.emm.data.HelloDb
import java.time.Instant
import java.util.UUID

class LocalDeviceIdentityProvider(
    private val db: HelloDb,
) {

    private val queries = db.localFirstQueries

    fun getOrCreateDeviceId(): String {
        val current = queries.selectLocalDeviceIdentity().executeAsOneOrNull()
        if (current != null) return current.deviceId

        val now = Instant.now().toEpochMilli()
        val deviceId = UUID.randomUUID().toString()
        val installId = UUID.randomUUID().toString()
        queries.upsertLocalDeviceIdentity(
            deviceId = deviceId,
            installId = installId,
            lamportCounter = 0L,
            createdAt = now,
            updatedAt = now,
        )
        return deviceId
    }
}
