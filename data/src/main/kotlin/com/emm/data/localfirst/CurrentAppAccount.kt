package com.emm.data.localfirst

import com.emm.data.HelloDb

fun HelloDb.currentAppAccountIdOrNull(): String? {
    return localFirstQueries
        .selectLocalAccountState()
        .executeAsOneOrNull()
        ?.appAccountId
        ?.takeIf(String::isNotBlank)
}

fun HelloDb.requireCurrentAppAccountId(): String {
    return requireNotNull(currentAppAccountIdOrNull()) {
        "Current app account is not available"
    }
}
