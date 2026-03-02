package com.emm.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class SyncStatusResponse(
    val success: Boolean,
    val message: String,
    val synced: SyncedResponse,
    val errors: List<ErrorResponse>
)

@Serializable
data class SyncedResponse(
    val decks: Int,
    val flashcards: Int,
    val examples: Int,
    val quotes: Int,
)

@Serializable
data class ErrorResponse(
    val type: String,
    val reason: String,
)

@Serializable
data class ExceptionResponse(
    val success: Boolean,
    val message: String,
)
