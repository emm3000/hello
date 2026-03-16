package com.emm.domain.sync

class EnsureLinkedIdentityUseCase(
    private val repository: PairingRepository,
) {
    suspend fun execute() {
        repository.ensureLinkedIdentity()
    }
}
