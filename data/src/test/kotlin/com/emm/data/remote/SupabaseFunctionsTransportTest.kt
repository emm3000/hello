package com.emm.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.MemoryCodeVerifierCache
import io.github.jan.supabase.auth.MemorySessionManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.functions.functions
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

class SupabaseFunctionsTransportTest {

    private val requestBody: JsonElement = buildJsonObject {
        put("input_type", JsonPrimitive("Word"))
        put("user_text", JsonPrimitive("give up"))
    }

    @Test
    fun `invoke posts the body to the function url with the session, api key and app check headers`() = runTest {
        var captured: HttpRequestData? = null
        val transport: FunctionsTransport = transport(
            status = HttpStatusCode.OK,
            content = "{\"ok\":true}",
            onRequest = { request -> captured = request },
        )

        val reply: FunctionsReply = transport.invoke(
            function = "generate-note",
            body = requestBody,
            appCheckToken = "app-check-token",
        )

        val request: HttpRequestData = requireNotNull(captured)
        assertEquals("/functions/v1/generate-note", request.url.encodedPath)
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("Bearer session-jwt", request.headers[HttpHeaders.Authorization])
        assertEquals("publishable-key", request.headers["apikey"])
        assertEquals("app-check-token", request.headers["X-Firebase-AppCheck"])
        assertEquals(ContentType.Application.Json, request.body.contentType)
        assertEquals(requestBody.toString(), (request.body as TextContent).text)
        assertEquals(FunctionsReply(status = 200, body = "{\"ok\":true}"), reply)
    }

    @Test
    fun `invoke returns a payment required reply instead of throwing`() = runTest {
        val content = "{\"error\":{\"code\":\"credits_exhausted\"," +
            "\"message\":\"Daily allowance reached\",\"reset_at\":\"2026-09-08T00:00:00Z\"}}"
        val transport: FunctionsTransport = transport(status = HttpStatusCode.PaymentRequired, content = content)

        val reply: FunctionsReply = transport.invoke(
            function = "generate-note",
            body = requestBody,
            appCheckToken = "app-check-token",
        )

        assertEquals(FunctionsReply(status = 402, body = content), reply)
    }

    @Test
    fun `invoke returns an unauthorized reply with the raw body`() = runTest {
        val transport: FunctionsTransport = transport(
            status = HttpStatusCode.Unauthorized,
            content = "Invalid JWT",
            contentType = ContentType.Text.Plain,
        )

        val reply: FunctionsReply = transport.invoke(
            function = "generate-note",
            body = requestBody,
            appCheckToken = "app-check-token",
        )

        assertEquals(FunctionsReply(status = 401, body = "Invalid JWT"), reply)
    }

    private suspend fun transport(
        status: HttpStatusCode,
        content: String,
        contentType: ContentType = ContentType.Application.Json,
        onRequest: (HttpRequestData) -> Unit = {},
    ): FunctionsTransport {
        val engine = MockEngine { request ->
            onRequest(request)
            respond(
                content = content,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, contentType.toString()),
            )
        }
        val client: SupabaseClient = createSupabaseClient(
            supabaseUrl = "http://localhost:54321",
            supabaseKey = "publishable-key",
        ) {
            httpEngine = engine
            install(Functions)
            install(Auth) {
                autoLoadFromStorage = false
                alwaysAutoRefresh = false
                enableLifecycleCallbacks = false
                sessionManager = MemorySessionManager()
                codeVerifierCache = MemoryCodeVerifierCache()
            }
        }
        client.auth.importAuthToken("session-jwt")
        return SupabaseFunctionsTransport(client.functions)
    }
}
