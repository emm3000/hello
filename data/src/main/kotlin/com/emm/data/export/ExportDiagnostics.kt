package com.emm.data.export

/**
 * Thrown when the backup file schema version is incompatible.
 */
class IncompatibleSchemaException(actualVersion: Int) : Exception(
    "Incompatible backup schema version: $actualVersion. Expected 1."
)

/**
 * Thrown when export fails at any point during streaming write.
 */
class ExportException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Thrown when import fails at any point: parsing error, schema validation, or DB write failure.
 */
class ImportException(message: String, cause: Throwable? = null) : Exception(message, cause)
