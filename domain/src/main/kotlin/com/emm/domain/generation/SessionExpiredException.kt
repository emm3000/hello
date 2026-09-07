package com.emm.domain.generation

class SessionExpiredException(
    cause: Throwable,
) : RuntimeException(cause.message, cause)
