package com.emm.domain.account

data class Account(
    val id: String,
    val isAnonymous: Boolean,
    val email: String?,
)
