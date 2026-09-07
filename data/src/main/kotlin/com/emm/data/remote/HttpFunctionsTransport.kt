package com.emm.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonElement

class HttpFunctionsTransport(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val publishableKey: String,
) : FunctionsTransport {

    override suspend fun invoke(
        function: String,
        body: JsonElement,
        accessToken: String,
        appCheckToken: String,
    ): FunctionsReply {
        val response: HttpResponse = httpClient.post("$baseUrl/functions/v1/$function") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            header(API_KEY_HEADER, publishableKey)
            header(APP_CHECK_HEADER, appCheckToken)
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }
        return FunctionsReply(status = response.status.value, body = response.bodyAsText())
    }

    private companion object {
        const val API_KEY_HEADER: String = "apikey"
        const val APP_CHECK_HEADER: String = "X-Firebase-AppCheck"
    }
}
