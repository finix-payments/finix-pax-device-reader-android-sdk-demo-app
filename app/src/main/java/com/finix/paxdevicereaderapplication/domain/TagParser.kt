package com.finix.paxdevicereaderapplication.domain

/**
 * Parses and validates the comma-separated `key:value` tag strings used throughout the app,
 * E.g. `"order:123, table:5"`.
 */
object TagParser {

    /** True when [input] is blank or a well-formed comma-separated list of `key:value` pairs. */
    fun isValid(input: String): Boolean {
        if (input.isBlank()) return true
        return input.split(",").all { pair ->
            val parts = pair.split(":")
            parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()
        }
    }

    /**
     * Converts a valid tag [input] into a map, or `null` when the input is blank or malformed.
     * A `null` result is what the SDK expects when there are no tags.
     */
    fun toMap(input: String): Map<String, String>? {
        if (input.isBlank() || !isValid(input)) return null
        return input.split(",")
            .associate { pair ->
                val (key, value) = pair.split(":").map(String::trim)
                key to value
            }
    }

    /** Renders a tag map back into the canonical `"key: value, key2: value2"` display form. */
    fun format(tags: Map<String, String>?): String =
        tags.orEmpty().entries.joinToString(", ") { "${it.key}: ${it.value}" }
}
