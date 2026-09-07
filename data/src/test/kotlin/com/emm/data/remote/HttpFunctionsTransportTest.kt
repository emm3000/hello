package com.emm.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.assertEquals
import org.junit.Test

class HttpFunctionsTransportTest {

    private val requestBody: JsonElement = buildJsonObject {
        put("input_type", JsonPrimitive("Word"))
        put("user_text", JsonPrimitive("give up"))
    }

    @Test
    fun `invoke posts the body to the function url with the session, api key and app check headers`() = runTest {
        var captured: HttpRequestData? = null
        val transport: FunctionsTransport = transport(
            status = HttpStatusCode.OK,
            content = "{\"success\":true}",
            onRequest = { request -> captured = request },
        )

        transport.invoke(
            function = "generate-note",
            body = requestBody,
            accessToken = "session-jwt",
            appCheckToken = "app-check-token",
        )

        val request: HttpRequestData = requireNotNull(captured)
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("https://hello.test/functions/v1/generate-note", request.url.toString())
        assertEquals("Bearer session-jwt", request.headers[HttpHeaders.Authorization])
        assertEquals("publishable-key", request.headers["apikey"])
        assertEquals("app-check-token", request.headers["X-Firebase-AppCheck"])
        assertEquals(ContentType.Application.Json, request.body.contentType)
        assertEquals(requestBody.toString(), (request.body as TextContent).text)
    }

    @Test
    fun `invoke returns a payment required reply instead of throwing`() = runTest {
        val content = "{\"success\":false,\"error\":{\"code\":\"credits_exhausted\"}}"
        val transport: FunctionsTransport = transport(status = HttpStatusCode.PaymentRequired, content = content)

        val reply: FunctionsReply = transport.invoke(
            function = "generate-note",
            body = requestBody,
            accessToken = "session-jwt",
            appCheckToken = "app-check-token",
        )

        assertEquals(HttpStatusCode.PaymentRequired.value, reply.status)
        assertEquals(content, reply.body)
    }

    private fun transport(
        status: HttpStatusCode,
        content: String,
        onRequest: (HttpRequestData) -> Unit = {},
    ): FunctionsTransport {
        val engine = MockEngine { request ->
            onRequest(request)
            respond(
                content = content,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        return HttpFunctionsTransport(
            httpClient = HttpClient(engine) { expectSuccess = false },
            baseUrl = "https://hello.test",
            publishableKey = "publishable-key",
        )
    }
}
