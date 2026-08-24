package com.finix.paxdevicereaderapplication.data.local

import com.finix.common.coreDeviceSdk.api.MerchantData
import com.finix.common.coreDeviceSdk.api.models.Environment
import kotlinx.serialization.Serializable

/**
 * Serializable representation of the merchant credentials, used for both the bundled
 * `assets/merchant_config.json` defaults and for persistence. Kept separate from the SDK's
 * [MerchantData] so the wire format is stable and independent of the SDK model.
 */
@Serializable
data class MerchantConfigDto(
    val deviceId: String = "",
    val merchantId: String = "",
    val userId: String = "",
    val password: String = "",
) {
    fun toMerchantData(env: Environment): MerchantData =
        MerchantData(
            merchantId = merchantId,
            deviceId = deviceId,
            env = env,
            userId = userId,
            password = password,
        )

    companion object {
        fun from(data: MerchantData): MerchantConfigDto =
            MerchantConfigDto(
                deviceId = data.deviceId,
                merchantId = data.merchantId,
                userId = data.userId,
                password = data.password,
            )
    }
}
