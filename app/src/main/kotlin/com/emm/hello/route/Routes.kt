package com.emm.hello.route

import kotlinx.serialization.Serializable

@Serializable
object Init

@Serializable
object Home

@Serializable
data class Detail(val wordId: String)