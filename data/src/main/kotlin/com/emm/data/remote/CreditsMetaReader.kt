package com.emm.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object CreditsMetaReader {

    fun remainingOrNull(body: String, json: Json): Int? {
        return json.decodeCreditsMetaOrNull(body)?.creditsRemaining
    }

    private fun Json.decodeCreditsMetaOrNull(body: String): CreditsMetaDto? {
        return try {
            decodeFromString<CreditsMetaEnvelopeDto>(body).meta
        } catch (ignored: SerializationException) {
            null
        } catch (ignored: IllegalArgumentException) {
            null
        }
    }
}

@Serializable
private data class CreditsMetaEnvelopeDto(
    val meta: CreditsMetaDto? = null,
)

@Serializable
private data class CreditsMetaDto(
    @SerialName("credits_remaining") val creditsRemaining: Int? = null,
)
