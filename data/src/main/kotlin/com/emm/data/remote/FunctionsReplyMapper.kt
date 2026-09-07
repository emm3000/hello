package com.emm.data.remote

import com.emm.domain.generation.AppCheckRejectedException
import com.emm.domain.generation.GenerationCreditsExhaustedException
import java.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object FunctionsReplyMapper {

    private const val OK: Int = 200
    private const val BAD_REQUEST: Int = 400
    private const val UNAUTHORIZED: Int = 401
    private const val PAYMENT_REQUIRED: Int = 402

    fun <T> map(reply: FunctionsReply, json: Json, onSuccess: (String) -> T): T {
        if (reply.status == OK) return onSuccess(reply.body)
        throw reply.toError(json)
    }

    private fun FunctionsReply.toError(json: Json): Throwable {
        val error: FunctionsErrorDto? = json.decodeErrorOrNull(body)
        return when (status) {
            UNAUTHORIZED -> AppCheckRejectedException(IllegalStateException(error?.code ?: body))
            PAYMENT_REQUIRED -> GenerationCreditsExhaustedException(resetAt = null)
            BAD_REQUEST -> IllegalArgumentException(error?.message ?: body)
            else -> IOException("generate function returned $status")
        }
    }

    private fun Json.decodeErrorOrNull(body: String): FunctionsErrorDto? {
        return try {
            decodeFromString<FunctionsErrorEnvelopeDto>(body).error
        } catch (ignored: SerializationException) {
            null
        } catch (ignored: IllegalArgumentException) {
            null
        }
    }
}

@Serializable
private data class FunctionsErrorEnvelopeDto(
    val error: FunctionsErrorDto? = null,
)

@Serializable
private data class FunctionsErrorDto(
    val code: String? = null,
    val message: String? = null,
)
