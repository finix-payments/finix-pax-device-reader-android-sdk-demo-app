package com.finix.paxdevicereaderapplication.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TagParserTest {

    @Test
    fun `blank input is considered valid and maps to null`() {
        assertTrue(TagParser.isValid(""))
        assertNull(TagParser.toMap(""))
    }

    @Test
    fun `well formed pairs are valid and parsed`() {
        assertTrue(TagParser.isValid("order:123, table:5"))
        assertEquals(
            mapOf("order" to "123", "table" to "5"),
            TagParser.toMap("order:123, table:5"),
        )
    }

    @Test
    fun `malformed pairs are invalid and map to null`() {
        assertFalse(TagParser.isValid("order"))
        assertFalse(TagParser.isValid("order:"))
        assertFalse(TagParser.isValid("a:b:c"))
        assertNull(TagParser.toMap("order"))
    }

    @Test
    fun `format round-trips a tag map`() {
        assertEquals("order: 123, table: 5", TagParser.format(mapOf("order" to "123", "table" to "5")))
        assertEquals("", TagParser.format(null))
    }
}
