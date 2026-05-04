package com.emm.domain.validation

class DomainValidationException(
    val issues: List<ValidationIssue>,
) : IllegalArgumentException("domain_validation_failed") {
    init {
        require(issues.isNotEmpty()) { "DomainValidationException requires at least one issue." }
    }
}
