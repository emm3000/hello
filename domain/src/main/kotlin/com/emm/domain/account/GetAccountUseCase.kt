package com.emm.domain.account

class GetAccountUseCase(private val repository: AccountRepository) {

    suspend operator fun invoke(): Account? = repository.currentAccount()
}
