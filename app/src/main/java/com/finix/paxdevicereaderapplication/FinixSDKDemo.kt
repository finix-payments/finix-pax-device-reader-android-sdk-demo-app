package com.finix.paxdevicereaderapplication

import android.app.Application
import com.finix.common.coreDeviceSdk.vendor.VendorBindings
import com.finix.paxlibrary.sdk.PaxTerminalDriver
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point. Registers the PAX vendor driver with the core SDK so that
 * `FinixTerminalSDK.createDevice(...)` can build PAX terminal devices, and bootstraps Hilt.
 */
@HiltAndroidApp
class FinixSDKDemo : Application() {
    override fun onCreate() {
        super.onCreate()
        VendorBindings.register(PaxTerminalDriver())
    }
}
