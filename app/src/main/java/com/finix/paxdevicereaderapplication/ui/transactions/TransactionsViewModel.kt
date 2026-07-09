package com.finix.paxdevicereaderapplication.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finix.common.coreDeviceSdk.api.MerchantData
import com.finix.common.coreDeviceSdk.api.models.Environment
import com.finix.common.coreDeviceSdk.api.terminal.TerminalDevice
import com.finix.common.coreDeviceSdk.api.transaction.SplitTransfer
import com.finix.common.coreDeviceSdk.api.transaction.TransactionHandle
import com.finix.common.coreDeviceSdk.api.transaction.TransactionRequest
import com.finix.common.coreDeviceSdk.api.transaction.TransactionType
import com.finix.common.coreDeviceSdk.api.transaction.TransactionUpdate
import com.finix.paxdevicereaderapplication.data.repository.ConfigRepository
import com.finix.paxdevicereaderapplication.domain.Money
import com.finix.paxdevicereaderapplication.domain.TagParser
import com.finix.paxdevicereaderapplication.domain.TransactionLogger
import com.finix.paxdevicereaderapplication.ui.state.TransactionsUiState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the transaction screen: loads and persists configuration, starts and cancels
 * transactions through the injected [TerminalDevice], and exposes a single immutable
 * [TransactionsUiState] plus a log stream.
 *
 * The [TerminalDevice] is provided via assisted injection because it depends on the hosting
 * [android.app.Activity], which only exists at the UI layer.
 */
@HiltViewModel(assistedFactory = TransactionsViewModel.Factory::class)
class TransactionsViewModel @AssistedInject constructor(
    private val configRepository: ConfigRepository,
    @Assisted private val device: TerminalDevice,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(device: TerminalDevice): TransactionsViewModel
    }

    private val logger = TransactionLogger()

    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

    /** The running transaction's cancellation handle, if any. */
    private var activeTransaction: TransactionHandle? = null

    val logText: StateFlow<String> = logger.text

    init {
        loadInitialConfiguration()
    }

    private fun loadInitialConfiguration() {
        viewModelScope.launch {
            val env = configRepository.currentEnvironment()
            val merchantData = configRepository.loadMerchantData(env)
            device.updateMerchantData(merchantData)
            _uiState.update {
                it.copy(
                    merchantData = merchantData,
                    availableEnvironments = configRepository.environments(),
                    splitTransfers = configRepository.loadSplitTransfers(env),
                    tags = configRepository.loadTags(env),
                )
            }
        }
    }

    // --- Configuration ----------------------------------------------------------------------

    /** Loads persisted credentials for [env], used by the configuration screen tabs. */
    suspend fun merchantDataFor(env: Environment): MerchantData =
        configRepository.loadMerchantData(env)

    fun saveMerchantData(updated: MerchantData) {
        viewModelScope.launch {
            configRepository.saveMerchantData(updated)
            device.updateMerchantData(updated)
            _uiState.update { it.copy(merchantData = updated) }
        }
    }

    fun saveTags(tags: String) {
        viewModelScope.launch {
            val env = currentEnv()
            configRepository.saveTags(env, tags)
            _uiState.update { it.copy(tags = tags) }
        }
    }

    fun saveSplitTransfers(splits: List<SplitTransfer>) {
        viewModelScope.launch {
            val env = currentEnv()
            configRepository.saveSplitTransfers(env, splits)
            _uiState.update { it.copy(splitTransfers = splits) }
        }
    }

    fun clearSplitTransfers() {
        viewModelScope.launch {
            val env = currentEnv()
            configRepository.clearSplitTransfers(env)
            _uiState.update { it.copy(splitTransfers = emptyList()) }
        }
    }

    // --- Transactions -----------------------------------------------------------------------

    fun startTransaction(
        amount: String,
        tip: String,
        surcharge: String,
        transactionType: TransactionType,
        idempotencyId: String? = null,
    ) {
        logger.log("Starting $transactionType…")
        viewModelScope.launch {
            runCatching {
                val request = TransactionRequest(
                    amount = Money.dollarsToCentsOrZero(amount),
                    transactionType = transactionType,
                    splitTransfers = _uiState.value.splitTransfers.ifEmpty { null },
                    tags = TagParser.toMap(_uiState.value.tags),
                    tipAmount = Money.dollarsToCentsOrZero(tip),
                    surchargeAmount = Money.dollarsToCentsOrZero(surcharge),
                    idempotencyId = idempotencyId,
                )
                device.startTransaction(request).also { activeTransaction = it }
            }.onSuccess { handle ->
                observeUpdates(handle, transactionType)
            }.onFailure { error ->
                logger.log("$transactionType failed to start: ${error.message}")
                setProcessing(false)
            }
        }
    }

    private suspend fun observeUpdates(handle: TransactionHandle, type: TransactionType) {
        runCatching {
            handle.updates.collect { update ->

                when (update) {
                    is TransactionUpdate.Processing -> {
                        logger.log("Transaction processing: ${update.step}")
                        setProcessing(true)
                    }

                    is TransactionUpdate.Retry -> {
                        logger.log("Retry (${update.failureType})")
                        setProcessing(true)
                    }

                    is TransactionUpdate.Success -> {
                        logger.log("$type approved: ${update.result.id}")
                        if (update.result.signatureRequired) {
                            showSignatureSheet()
                        }
                        setProcessing(false)
                    }

                    is TransactionUpdate.Error -> {
                        logger.log("$type error: ${update.result.failureMessage}")
                        setProcessing(false)
                    }
                }
            }
        }.onFailure { error ->
            logger.log("$type failed: ${error.message}")
            setProcessing(false)
        }
        activeTransaction = null
    }

    fun cancelTransaction() {
        val handle = activeTransaction ?: return
        viewModelScope.launch {
            logger.log("Cancelling transaction…")
            handle.cancel()
            activeTransaction = null
        }
    }

    // --- Signature --------------------------------------------------------------------------

    private fun showSignatureSheet() {
        _uiState.update { it.copy(isSignatureSheetVisible = true) }
    }

    fun submitSignature(pngEncodedBase64: String) {
        if (pngEncodedBase64.isBlank()) {
            logger.log("Signature was empty and was not submitted")
            dismissSignatureSheet()
            return
        }
        // The current SDK snapshot captures the signature during the on-device flow; the app only
        // needs to acknowledge and dismiss. When a signature-upload API is exposed, call it here.
        logger.log("Signature captured")
        dismissSignatureSheet()
    }

    fun dismissSignatureSheet() {
        _uiState.update { it.copy(isSignatureSheetVisible = false) }
    }

    // --- Logs -------------------------------------------------------------------------------

    fun clearLogs() = logger.clear()

    // --- Helpers ----------------------------------------------------------------------------

    private fun currentEnv(): Environment = _uiState.value.environment

    private fun setProcessing(processing: Boolean) {
        _uiState.update { it.copy(isProcessing = processing) }
    }
}
