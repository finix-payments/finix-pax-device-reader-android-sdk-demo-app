package com.finix.paxdevicereaderapplication.device

import android.app.Activity
import com.finix.common.coreDeviceSdk.api.FinixTerminalSDK
import com.finix.common.coreDeviceSdk.api.MerchantData
import com.finix.common.coreDeviceSdk.api.terminal.TerminalDevice
import com.finix.common.coreDeviceSdk.api.terminal.TerminalOptions

/**
 * Creates [TerminalDevice] instances. Abstracting the SDK entry point behind an interface keeps the
 * rest of the app decoupled from [FinixTerminalSDK] and makes the device creation seam easy to fake
 * in tests.
 *
 * The SDK requires an [Activity] (embedded terminals render on-device UI), so construction happens
 * from the activity rather than being injected directly into the ViewModel.
 */
interface TerminalDeviceFactory {
    fun create(
        activity: Activity,
        merchantData: MerchantData,
        options: TerminalOptions = TerminalOptions(),
    ): TerminalDevice
}

class FinixTerminalDeviceFactory : TerminalDeviceFactory {
    override fun create(
        activity: Activity,
        merchantData: MerchantData,
        options: TerminalOptions,
    ): TerminalDevice =
        FinixTerminalSDK.createDevice(
            activity = activity,
            merchantData = merchantData,
            options = options,
        )
}
