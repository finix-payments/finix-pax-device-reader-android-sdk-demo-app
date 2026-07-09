package com.finix.paxdevicereaderapplication.domain

import java.util.Locale
import kotlin.math.roundToLong

/**
 * Helpers for converting between a human-entered dollar string (e.g. "3.14") and the minor-unit
 * amount in cents (e.g., 314) expected by the Finix SDK, and back again for display.
 *
 * All conversions are pure functions, so they can be unit-tested without Android dependencies.
 */
object Money {

    /** Matches an in-progress dollar amount with up to two decimal places, e.g. "", "3", "3.1", "3.14". */
    private val PARTIAL_AMOUNT = Regex("""^\d*(\.\d{0,2})?$""")

    /**
     * Returns [candidate] if it is a valid partial dollar amount, otherwise [fallback].
     * Useful for filtering `TextField` input as the user types.
     */
    fun sanitizeInput(candidate: String, fallback: String): String =
        if (PARTIAL_AMOUNT.matches(candidate)) candidate else fallback

    /** Parses a dollar string into cents, or `null` if it is blank or not a number. */
    fun dollarsToCents(dollars: String): Long? =
        dollars.trim().toDoubleOrNull()?.let { (it * 100).roundToLong() }

    /** Parses a dollar string into cents, defaulting to `0` when it cannot be parsed. */
    fun dollarsToCentsOrZero(dollars: String): Long = dollarsToCents(dollars) ?: 0L

    /** Formats a cent amount as a display string, e.g., 314 -> "$3.14". */
    fun formatCents(cents: Long): String =
        String.format(Locale.US, "$%,.2f", cents / 100.0)
}
