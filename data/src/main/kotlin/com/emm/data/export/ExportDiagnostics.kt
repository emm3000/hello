package com.emm.data.export

class IncompatibleSchemaException(actualVersion: Int, supportedVersions: IntRange = 1..2) : Exception(
    "Incompatible backup schema version: $actualVersion. Expected one of $supportedVersions."
)

class ExportException(message: String, cause: Throwable? = null) : Exception(message, cause)

class ImportException(message: String, cause: Throwable? = null) : Exception(message, cause)
