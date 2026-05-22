package com.emm.hello.newfeatures.card

import com.emm.domain.generation.GenerationQuotaExceededException
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.TimeoutCancellationException

internal data class ClassifiedError(
    val title: String,
    val message: String,
    val quotaResetAt: Instant? = null,
)

internal object NewCardErrorClassifier {

    fun classifyGenerationFailure(error: Throwable, fallbackMessage: String): ClassifiedError {
        error.findQuotaExceeded()?.let { return it.toClassifiedError() }
        if (error.isNetworkRelated()) {
            return ClassifiedError(
                title = "Sin conexión",
                message = "No pudimos contactar al asistente. Verificá tu conexión y reintentá.",
            )
        }
        return ClassifiedError(
            title = "Respuesta inválida de IA",
            message = error.message ?: fallbackMessage,
        )
    }

    fun classifyRegenerationFailure(
        error: Throwable,
        failureTitle: String,
        fallbackMessage: String,
    ): ClassifiedError {
        error.findQuotaExceeded()?.let { return it.toClassifiedError() }
        if (error.isNetworkRelated()) {
            return ClassifiedError(
                title = "Sin conexión",
                message = "No pudimos contactar al asistente. Verificá tu conexión y reintentá.",
            )
        }
        return ClassifiedError(
            title = failureTitle,
            message = error.message ?: fallbackMessage,
        )
    }

    private fun Throwable.findQuotaExceeded(): GenerationQuotaExceededException? {
        var current: Throwable? = this
        while (current != null) {
            if (current is GenerationQuotaExceededException) return current
            current = current.cause?.takeIf { it !== current }
        }
        return null
    }

    private fun GenerationQuotaExceededException.toClassifiedError(): ClassifiedError {
        return ClassifiedError(
            title = "Llegamos al límite diario de IA.",
            message = "Llegaste al máximo de $limit generaciones por día. Volvé a intentarlo mañana.",
            quotaResetAt = resetAt,
        )
    }

    private fun Throwable.isNetworkRelated(): Boolean {
        var current: Throwable? = this
        while (current != null) {
            if (current is TimeoutCancellationException || current is IOException) return true
            val name = current::class.qualifiedName.orEmpty()
            if (NETWORK_ERROR_CLASS_HINTS.any { hint -> name.contains(hint, ignoreCase = true) }) return true
            val text = current.message.orEmpty()
            if (text.isNotBlank() && NETWORK_MESSAGE_HINTS.any { hint -> text.contains(hint, ignoreCase = true) }) {
                return true
            }
            current = current.cause?.takeIf { it !== current }
        }
        return false
    }

    private val NETWORK_ERROR_CLASS_HINTS = listOf(
        "UnknownHost",
        "ConnectException",
        "SocketTimeout",
        "ServerException",
        "ServiceUnavailable",
        "QuotaExceeded",
    )

    private val NETWORK_MESSAGE_HINTS = listOf(
        "timeout",
        "timed out",
        "unable to resolve",
        "network",
        "connection",
        "unreachable",
    )
}
