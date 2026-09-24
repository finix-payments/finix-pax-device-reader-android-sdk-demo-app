package com.finix.paxdevicereaderapplication.domain

import com.finix.common.coreDeviceSdk.api.MerchantData
import com.finix.common.coreDeviceSdk.api.transaction.ReferencedRefundRequest
import com.finix.common.coreDeviceSdk.api.transaction.SplitTransfer

/**
 * Outcome of validating a form. [errors] maps a field key (matching the keys used by the UI, e.g.
 * "merchantId") to a human-readable message.
 */
data class ValidationResult(
    val errors: Map<String, String> = emptyMap(),
) {
    val isValid: Boolean get() = errors.isEmpty()
}

/**
 * Small DSL for accumulating field errors while validating a form.
 * `require(condition) { "field" to "message" }` records the error only when the condition fails.
 */
private class ErrorCollector {
    private val errors = mutableMapOf<String, String>()

    inline fun require(condition: Boolean, error: () -> Pair<String, String>) {
        if (!condition) {
            val (field, message) = error()
            putIfAbsent(field, message)
        }
    }

    fun putIfAbsent(field: String, message: String) {
        errors.putIfAbsent(field, message)
    }

    fun result() = ValidationResult(errors.toMap())
}

private inline fun validate(block: ErrorCollector.() -> Unit): ValidationResult =
    ErrorCollector().apply(block).result()

/** Finix identifier prefixes. */
private object IdPrefix {
    val MERCHANT = Regex("^MU[a-zA-Z0-9]+$")
    val DEVICE = Regex("^DV[a-zA-Z0-9]+$")
    val TRANSFER = Regex("^TR[a-zA-Z0-9]+$")
    const val MIN_PASSWORD_LENGTH = 8
}

/** Validates the merchant credentials entered on the configuration screen. */
object MerchantConfigValidator {
    fun validate(data: MerchantData): ValidationResult = validate {
        require(data.deviceId.isNotBlank()) { "deviceId" to "Device ID is required" }
        require(data.deviceId.isBlank() || data.deviceId.matches(IdPrefix.DEVICE)) {
            "deviceId" to "Device ID is not valid"
        }

        require(data.merchantId.isNotBlank()) { "merchantId" to "Merchant ID is required" }
        require(data.merchantId.isBlank() || data.merchantId.matches(IdPrefix.MERCHANT)) {
            "merchantId" to "Merchant ID is not valid"
        }

        require(data.userId.isNotBlank()) { "userId" to "Username is required" }
        require(data.password.length >= IdPrefix.MIN_PASSWORD_LENGTH) {
            "password" to "Password must be at least ${IdPrefix.MIN_PASSWORD_LENGTH} characters"
        }
    }
}

/** Validates a single split-transfer entry on the "Others" screen. */
object SplitTransferValidator {
    fun validate(split: SplitTransfer): ValidationResult = validate {
        require(split.merchantId.isNotBlank()) { "merchantId" to "Merchant ID is required" }
        require(split.merchantId.isBlank() || split.merchantId.matches(IdPrefix.MERCHANT)) {
            "merchantId" to "Merchant ID is not valid"
        }
        require(split.amount > 0) { "amount" to "Amount must be greater than 0" }
        require(TagParser.isValid(TagParser.format(split.tags))) {
            "tags" to "Invalid format. Use key:value, key2:value2"
        }
    }
}

/** Validates the referenced-refund form. */
object ReferencedRefundValidator {
    fun validate(request: ReferencedRefundRequest): ValidationResult = validate {
        require(request.transferId.isNotBlank()) { "transferId" to "Transfer ID is required" }
        require(request.transferId.isBlank() || request.transferId.matches(IdPrefix.TRANSFER)) {
            "transferId" to "Transfer ID is not valid"
        }
        require(request.amount > 0) { "amount" to "Amount must be greater than 0" }
    }
}
