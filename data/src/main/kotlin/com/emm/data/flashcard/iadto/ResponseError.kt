package com.emm.data.flashcard.iadto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ResponseError(

    @SerialName("input")
    val input: String,

    @SerialName("message")
    val message: String,
)