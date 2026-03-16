package com.emm.domain.sync

class RedeemPairingCodeUseCase(
    private val repository: PairingRepository,
) {
    suspend fun execute(code: String) {
        repository.redeemPairingCode(code)
    }
}
