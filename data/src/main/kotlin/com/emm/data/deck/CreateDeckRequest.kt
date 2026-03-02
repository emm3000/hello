package com.emm.data.deck

import kotlinx.serialization.Serializable

@Serializable
data class CreateDeckRequest(
    val id: String,
    val name: String,
    val description: String,
    val createdAt: Long,
    val updatedAt: Long
)
