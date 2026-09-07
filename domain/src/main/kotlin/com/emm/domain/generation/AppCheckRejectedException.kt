package com.emm.domain.generation

class AppCheckRejectedException(
    cause: Throwable,
) : RuntimeException(cause.message, cause)
