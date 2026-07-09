package com.finix.paxdevicereaderapplication.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {

    @Test
    fun `dollarsToCents converts whole and fractional amounts`() {
        assertEquals(314L, Money.dollarsToCents("3.14"))
        assertEquals(500L, Money.dollarsToCents("5"))
        assertEquals(0L, Money.dollarsToCents("0"))
    }

    @Test
    fun `dollarsToCents returns null for blank or invalid input`() {
        assertNull(Money.dollarsToCents(""))
        assertNull(Money.dollarsToCents("abc"))
    }

    @Test
    fun `dollarsToCentsOrZero falls back to zero`() {
        assertEquals(0L, Money.dollarsToCentsOrZero(""))
        assertEquals(199L, Money.dollarsToCentsOrZero("1.99"))
    }

    @Test
    fun `sanitizeInput keeps valid partial amounts and rejects the rest`() {
        assertEquals("3.1", Money.sanitizeInput("3.1", "3"))
        assertEquals("", Money.sanitizeInput("", "3"))
        assertEquals("3.14", Money.sanitizeInput("3.145", "3.14"))
        assertEquals("3.14", Money.sanitizeInput("3.1x", "3.14"))
    }

    @Test
    fun `formatCents renders a currency string`() {
        assertEquals("$3.14", Money.formatCents(314))
        assertEquals("$0.00", Money.formatCents(0))
        assertEquals("$1,000.00", Money.formatCents(100_000))
    }
}
