package com.emm.data.localfirst

import com.emm.data.HelloDb
import java.time.Instant
import java.util.UUID

class LocalDeviceIdentityProvider(
    private val db: HelloDb,
) {

    private val queries = db.localFirstQueries

    fun getOrCreateDeviceId(): String {
        val now = Instant.now().toEpochMilli()
        // INSERT OR IGNORE is atomic: if the row already exists the call is a no-op,
        // so concurrent calls cannot produce two different device IDs.
        queries.insertDeviceIdentityIfAbsent(
            deviceId = UUID.randomUUID().toString(),
            installId = UUID.randomUUID().toString(),
            createdAt = now,
            updatedAt = now,
        )
        return queries.selectLocalDeviceIdentity().executeAsOne().deviceId
    }
}
