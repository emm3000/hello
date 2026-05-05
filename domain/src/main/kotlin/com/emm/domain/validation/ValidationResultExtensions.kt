package com.emm.domain.validation

fun <T> ValidationResult<T>.requireValid(): T {
    if (!isValid) throw DomainValidationException(errors)
    return value
}
