package com.emm.data.remote

import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.functions.Functions
import io.ktor.client.plugins.timeout
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonElement

class SupabaseFunctionsTransport(
    private val functions: Functions,
) : FunctionsTransport {

    override suspend fun invoke(
        function: String,
        body: JsonElement,
        appCheckToken: String,
    ): FunctionsReply {
        return try {
            val response: HttpResponse = functions.invoke(function) {
                header(APP_CHECK_HEADER, appCheckToken)
                contentType(ContentType.Application.Json)
                setBody(body.toString())
                timeout { requestTimeoutMillis = REQUEST_TIMEOUT_MS }
            }
            FunctionsReply(status = response.status.value, body = response.bodyAsText())
        } catch (rejected: RestException) {
            FunctionsReply(status = rejected.statusCode, body = rejected.error)
        }
    }

    private companion object {
        const val APP_CHECK_HEADER: String = "X-Firebase-AppCheck"
        const val REQUEST_TIMEOUT_MS: Long = 100_000L
    }
}
