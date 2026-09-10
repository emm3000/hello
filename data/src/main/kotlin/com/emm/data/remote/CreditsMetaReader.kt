package com.emm.data.remote

import com.emm.domain.generation.GenerationCredits
import java.time.Instant
import java.time.format.DateTimeParseException
import kotlinx.serialization.SerialName
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object CreditsMetaReader {

    fun readOrNull(body: String, json: Json): GenerationCredits? {
        return json.decodeCreditsMetaOrNull(body)?.toCreditsOrNull()
    }

    private fun CreditsMetaDto.toCreditsOrNull(): GenerationCredits? {
        val balance: Int = creditsRemaining ?: return null
        val reset: Instant = resetAt?.let(::parseInstantOrNull) ?: return null
        return GenerationCredits(remaining = balance.coerceAtLeast(0), resetAt = reset)
    }

    private fun parseInstantOrNull(raw: String): Instant? {
        return try {
            Instant.parse(raw)
        } catch (ignored: DateTimeParseException) {
            null
        }
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
    @SerialName("reset_at") val resetAt: String? = null,
)
