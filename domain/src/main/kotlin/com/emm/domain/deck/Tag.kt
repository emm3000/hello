package com.emm.domain.deck

data class Tag(val value: String) {
    init {
        require(value.isNotBlank()) { "Tag value must not be blank." }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Tag) return false
        return value.equals(other.value, ignoreCase = true)
    }

    override fun hashCode(): Int {
        return value.lowercase().hashCode()
    }

    override fun toString(): String = "Tag(value=$value)"
}
