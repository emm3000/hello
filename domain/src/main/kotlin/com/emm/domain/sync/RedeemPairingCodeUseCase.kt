package com.emm.domain.sync

class RedeemPairingCodeUseCase(
    private val repository: PairingRepository,
) {
    suspend operator fun invoke(code: String) {
        repository.redeemPairingCode(code)
    }
}
