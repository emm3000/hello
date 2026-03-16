package com.emm.domain.sync

class EnsureLinkedIdentityUseCase(
    private val repository: PairingRepository,
) {
    suspend operator fun invoke() {
        repository.ensureLinkedIdentity()
    }
}
