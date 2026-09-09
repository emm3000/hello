package com.emm.domain.account

sealed interface AccountLinkResult {
    val account: Account

    data class Linked(override val account: Account) : AccountLinkResult
    data class SwitchedToExisting(override val account: Account) : AccountLinkResult
}
