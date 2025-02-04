package com.emm.hello.page

import kotlinx.serialization.Serializable

@Serializable
data class WordJson(
    val id: String,
    val word: String,
    val date: String,
)