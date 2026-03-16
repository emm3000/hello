package com.emm.domain.sync

class CreatePairingSessionUseCase(
    private val repository: PairingRepository,
) {
    suspend fun execute(ttlMinutes: Int = 10): PairingSession {
        return repository.createPairingSession(ttlMinutes)
    }
}
