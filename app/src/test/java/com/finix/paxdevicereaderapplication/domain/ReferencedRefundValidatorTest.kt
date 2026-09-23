package com.finix.paxdevicereaderapplication.domain

import com.finix.common.coreDeviceSdk.api.transaction.ReferencedRefundRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReferencedRefundValidatorTest {

    private fun request(
        transferId: String = "TRabc123",
        amount: Long = 314,
    ) = ReferencedRefundRequest(amount = amount, transferId = transferId)

    @Test
    fun `accepts a well-formed transfer id and amount`() {
        assertTrue(ReferencedRefundValidator.validate(request()).isValid)
    }

    @Test
    fun `rejects a blank transfer id`() {
        val result = ReferencedRefundValidator.validate(request(transferId = ""))

        assertFalse(result.isValid)
        assertEquals("Transfer ID is required", result.errors["transferId"])
    }

    @Test
    fun `reports only the required error for a blank transfer id`() {
        // The format check is skipped when the field is empty, so the user sees one
        // message rather than two contradictory ones.
        val result = ReferencedRefundValidator.validate(request(transferId = "   "))

        assertEquals(1, result.errors.size)
    }

    @Test
    fun `rejects a transfer id without the TR prefix`() {
        val result = ReferencedRefundValidator.validate(request(transferId = "MUabc123"))

        assertFalse(result.isValid)
        assertEquals("Transfer ID is not valid", result.errors["transferId"])
    }

    @Test
    fun `rejects a transfer id containing punctuation`() {
        assertFalse(ReferencedRefundValidator.validate(request(transferId = "TR-abc")).isValid)
    }

    @Test
    fun `rejects a bare TR prefix with no identifier`() {
        assertFalse(ReferencedRefundValidator.validate(request(transferId = "TR")).isValid)
    }

    @Test
    fun `rejects a zero amount`() {
        val result = ReferencedRefundValidator.validate(request(amount = 0))

        assertFalse(result.isValid)
        assertEquals("Amount must be greater than 0", result.errors["amount"])
    }

    @Test
    fun `rejects a negative amount`() {
        assertFalse(ReferencedRefundValidator.validate(request(amount = -100)).isValid)
    }

    @Test
    fun `reports both fields when both are wrong`() {
        val result = ReferencedRefundValidator.validate(request(transferId = "", amount = 0))

        assertEquals(2, result.errors.size)
    }
}
