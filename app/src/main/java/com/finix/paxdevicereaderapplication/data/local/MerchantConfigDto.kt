package com.finix.paxdevicereaderapplication.data.local

import com.finix.common.coreDeviceSdk.api.MerchantData
import com.finix.common.coreDeviceSdk.api.models.Environment
import com.finix.common.coreDeviceSdk.api.transaction.Country
import kotlinx.serialization.Serializable

/**
 * Serializable representation of the merchant credentials, used for both the bundled
 * `assets/merchant_config.json` defaults and for persistence. Kept separate from the SDK's
 * [MerchantData] so the wire format is stable and independent of the SDK model.
 */
@Serializable
data class MerchantConfigDto(
    val applicationId: String = "",
    val deviceId: String = "",
    val merchantId: String = "",
    val mid: String = "",
    val userId: String = "",
    val password: String = "",
) {
    fun toMerchantData(env: Environment, country: Country = Country.USA): MerchantData =
        MerchantData(
            merchantId = merchantId,
            mid = mid,
            deviceId = deviceId,
            applicationId = applicationId,
            env = env,
            userId = userId,
            password = password,
            country = country,
        )

    companion object {
        fun from(data: MerchantData): MerchantConfigDto =
            MerchantConfigDto(
                applicationId = data.applicationId,
                deviceId = data.deviceId,
                merchantId = data.merchantId,
                mid = data.mid,
                userId = data.userId,
                password = data.password,
            )
    }
}
