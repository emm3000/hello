package com.emm.hello.features.backup

import kotlinx.serialization.Serializable

@Serializable
data class WordJson(
    val id: String,
    val word: String,
    val createdAt: Long,
)