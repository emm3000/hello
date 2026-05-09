package com.emm.data.export

class IncompatibleSchemaException(actualVersion: Int) : Exception(
    "Incompatible backup schema version: $actualVersion. Expected 1."
)

class ExportException(message: String, cause: Throwable? = null) : Exception(message, cause)

class ImportException(message: String, cause: Throwable? = null) : Exception(message, cause)
