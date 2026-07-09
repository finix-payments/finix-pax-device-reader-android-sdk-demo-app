package com.finix.paxdevicereaderapplication.data.local

import com.finix.common.coreDeviceSdk.api.transaction.SplitTransfer
import kotlinx.serialization.Serializable

/**
 * Serializable mirror of the SDK's [SplitTransfer], used to persist split-transfer lists.
 * The SDK model itself is not annotated for kotlinx.serialization, so we map to/from this DTO.
 */
@Serializable
data class SplitTransferDto(
    val merchantId: String,
    val amount: Long,
    val tags: Map<String, String>? = null,
    val fee: Long? = null,
) {
    fun toDomain(): SplitTransfer =
        SplitTransfer(merchantId = merchantId, amount = amount, tags = tags, fee = fee)

    companion object {
        fun from(split: SplitTransfer): SplitTransferDto =
            SplitTransferDto(
                merchantId = split.merchantId,
                amount = split.amount,
                tags = split.tags,
                fee = split.fee,
            )
    }
}
