package com.finix.paxdevicereaderapplication.ui.state

import com.finix.common.coreDeviceSdk.api.MerchantData
import com.finix.common.coreDeviceSdk.api.models.Environment
import com.finix.common.coreDeviceSdk.api.transaction.SplitTransfer

/**
 * The complete state the transaction screen renders from. Held immutably and emitted as a single
 * [kotlinx.coroutines.flow.StateFlow] so the UI has one source of truth.
 */
data class TransactionsUiState(
    val merchantData: MerchantData = EMPTY_MERCHANT_DATA,
    val availableEnvironments: List<Environment> = emptyList(),
    val splitTransfers: List<SplitTransfer> = emptyList(),
    val tags: String = "",
    val isProcessing: Boolean = false,
    val isSignatureSheetVisible: Boolean = false,
    val isReferencedRefundDialogVisible: Boolean = false,
) {
    val environment: Environment get() = merchantData.env
    val hasSplitTransfers: Boolean get() = splitTransfers.isNotEmpty()

    private companion object {
        val EMPTY_MERCHANT_DATA = MerchantData(
            merchantId = "",
            deviceId = "",
            env = Environment.PROD,
            userId = "",
            password = ""
        )
    }
}
