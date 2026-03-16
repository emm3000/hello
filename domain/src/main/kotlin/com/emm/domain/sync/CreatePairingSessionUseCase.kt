package com.emm.domain.sync

class CreatePairingSessionUseCase(
    private val repository: PairingRepository,
) {
    suspend operator fun invoke(ttlMinutes: Int = 10): PairingSession {
        return repository.createPairingSession(ttlMinutes)
    }
}
