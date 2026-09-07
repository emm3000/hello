package com.emm.data.remote

import kotlinx.serialization.json.JsonElement

interface FunctionsTransport {

    suspend fun invoke(
        function: String,
        body: JsonElement,
        appCheckToken: String,
    ): FunctionsReply
}

data class FunctionsReply(
    val status: Int,
    val body: String,
)
