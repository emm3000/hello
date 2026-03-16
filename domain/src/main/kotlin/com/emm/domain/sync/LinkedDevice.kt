package com.emm.domain.sync

data class LinkedDevice(
    val id: String,
    val deviceName: String? = null,
    val platform: String? = null,
    val createdAt: String,
    val lastSeenAt: String? = null,
    val revokedAt: String? = null,
    val isCurrent: Boolean,
)
