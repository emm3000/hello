package com.emm.domain.seed

// ensureSeeded runs on every launch, so implementations must be idempotent.
interface SeedDataInitializer {
    suspend fun ensureSeeded()
}
