package com.emm.hello.core

import kotlinx.serialization.Serializable

@Serializable
object Home

@Serializable
object Main

@Serializable
data class Detail(val wordId: String)

@Serializable
object AddWord

@Serializable
object Backup