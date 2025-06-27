package com.emm.data.flashcard.iadto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeminiErrorDto(

    @SerialName("error")
    val responseError: ResponseError,

    @SerialName("success")
    val success: Boolean
)