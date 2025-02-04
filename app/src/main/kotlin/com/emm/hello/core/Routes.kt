package com.emm.hello.core

import kotlinx.serialization.Serializable

@Serializable
object Init

@Serializable
object Home

@Serializable
data class Detail(val wordId: String)

@Serializable
object AddWord

@Serializable
object Backup