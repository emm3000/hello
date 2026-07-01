package com.emm.domain.seed

/**
 * Seeds first-run example content so a brand-new install does not open on an empty Dashboard.
 *
 * Implementations MUST be idempotent and safe to call on every launch: the seed happens at most
 * once, and only for a genuinely new install (no existing decks).
 */
interface SeedDataInitializer {
    suspend fun ensureSeeded()
}
