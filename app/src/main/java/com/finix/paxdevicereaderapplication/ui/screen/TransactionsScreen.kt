package com.finix.paxdevicereaderapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finix.common.coreDeviceSdk.api.transaction.SplitTransfer
import com.finix.common.coreDeviceSdk.api.transaction.TransactionType
import com.finix.paxdevicereaderapplication.domain.Money
import com.finix.paxdevicereaderapplication.ui.components.AmountField
import com.finix.paxdevicereaderapplication.ui.components.LabeledTextField
import com.finix.paxdevicereaderapplication.ui.signature.SignatureBottomSheet
import com.finix.paxdevicereaderapplication.ui.transactions.TransactionsViewModel

private enum class ActiveSheet { NONE, CONFIGURATION, OTHER }

private val TRANSACTION_TYPES = listOf(
    "Sale" to TransactionType.SALE,
    "Auth" to TransactionType.AUTHORIZATION,
    "Refund" to TransactionType.REFUND,
)

@Composable
fun TransactionsScreen(viewModel: TransactionsViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val logText by viewModel.logText.collectAsStateWithLifecycle()

    var activeSheet by remember { mutableStateOf(ActiveSheet.NONE) }
    var amount by remember { mutableStateOf("3.14") }
    var tip by remember { mutableStateOf("0") }
    var surcharge by remember { mutableStateOf("0") }
    var idempotencyId by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TransactionsAppBar(
                onOpenConfiguration = { activeSheet = ActiveSheet.CONFIGURATION },
                onOpenOther = { activeSheet = ActiveSheet.OTHER },
                onOpenReferencedRefund = viewModel::showReferencedRefundDialog,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 15.dp, vertical = 15.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Selected environment: ${uiState.environment.name}",
                    style = TextStyle(fontStyle = FontStyle.Italic),
                )
            }

            TransactionSection(
                amount = amount,
                tip = tip,
                surcharge = surcharge,
                idempotencyId = idempotencyId,
                tags = uiState.tags,
                splitTransfers = uiState.splitTransfers,
                onAmountChange = { amount = it },
                onTipChange = { tip = it },
                onSurchargeChange = { surcharge = it },
                onIdempotencyIdChange = { idempotencyId = it },
                onTransactionClick = { type ->
                    viewModel.startTransaction(
                        amount = amount,
                        tip = tip,
                        surcharge = surcharge,
                        transactionType = type,
                        idempotencyId = idempotencyId.ifBlank { null },
                    )
                },
            )

            LogSection(logs = logText, onClearLogs = viewModel::clearLogs)
        }
    }

    when (activeSheet) {
        ActiveSheet.CONFIGURATION -> ConfigurationSheet(
            viewModel = viewModel,
            environments = uiState.availableEnvironments,
            initialEnvironment = uiState.environment,
            onDismiss = { activeSheet = ActiveSheet.NONE },
        )

        ActiveSheet.OTHER -> OtherSheet(
            initialTags = uiState.tags,
            initialSplits = uiState.splitTransfers,
            primaryMerchantId = uiState.merchantData.merchantId,
            onSave = { tags, splits ->
                viewModel.saveTags(tags)
                if (splits.isEmpty()) viewModel.clearSplitTransfers()
                else viewModel.saveSplitTransfers(splits)
            },
            onDismiss = { activeSheet = ActiveSheet.NONE },
        )

        ActiveSheet.NONE -> Unit
    }

    if (uiState.isReferencedRefundDialogVisible) {
        ReferencedRefundDialog(
            initialAmount = amount,
            onDismiss = viewModel::dismissReferencedRefundDialog,
            onConfirm = viewModel::initiateReferencedRefund,
        )
    }

    if (uiState.isSignatureSheetVisible) {
        SignatureBottomSheet(
            onConfirm = viewModel::submitSignature,
            onDismiss = viewModel::dismissSignatureSheet,
        )
    }

    if (uiState.isProcessing) {
        ProcessingOverlay(onCancel = viewModel::cancelTransaction)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionsAppBar(
    onOpenConfiguration: () -> Unit,
    onOpenOther: () -> Unit,
    onOpenReferencedRefund: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Finix")
            }
        },
        actions = {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Menu")
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Configurations") },
                    onClick = {
                        menuExpanded = false
                        onOpenConfiguration()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Others") },
                    onClick = {
                        menuExpanded = false
                        onOpenOther()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Referenced Refund") },
                    onClick = {
                        menuExpanded = false
                        onOpenReferencedRefund()
                    },
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}

@Composable
private fun TransactionSection(
    amount: String,
    tip: String,
    surcharge: String,
    idempotencyId: String,
    tags: String,
    splitTransfers: List<SplitTransfer>,
    onAmountChange: (String) -> Unit,
    onTipChange: (String) -> Unit,
    onSurchargeChange: (String) -> Unit,
    onIdempotencyIdChange: (String) -> Unit,
    onTransactionClick: (TransactionType) -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Spacer(modifier = Modifier.height(18.dp))
    Text("TRANSACTION", style = MaterialTheme.typography.labelLarge)

    Card {
        Column {
            AmountField("Amount:", amount, onAmountChange)
            AmountField("Tip:", tip, onTipChange)
            AmountField("Surcharge:", surcharge, onSurchargeChange)
            LabeledTextField("Idempotency ID:", idempotencyId, onIdempotencyIdChange)

            TransactionExtras(tags = tags, splitTransfers = splitTransfers)

            Row(
                modifier = Modifier.padding(5.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TRANSACTION_TYPES.forEach { (label, type) ->
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            keyboardController?.hide()
                            onTransactionClick(type)
                        },
                        shape = RoundedCornerShape(7.dp),
                    ) {
                        Text(label)
                    }
                }
            }
        }
    }
}

/** Read-only summary of the currently configured tags and split transfers. */
@Composable
private fun TransactionExtras(tags: String, splitTransfers: List<SplitTransfer>) {
    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        if (tags.isNotEmpty()) {
            Text("\nTags: $tags")
        }

        val visibleSplits = splitTransfers.filter { it.merchantId.isNotEmpty() && it.amount > 0 }
        if (visibleSplits.isNotEmpty()) {
            Text("Split Merchants:")
            visibleSplits.forEach { split ->
                val fee = split.fee?.takeIf { it > 0 }
                    ?.let { ", Fee: ${Money.formatCents(it)}" }
                    .orEmpty()
                Text("Merchant: ${split.merchantId}, Amount: ${Money.formatCents(split.amount)}$fee")
            }
        }
    }
}

@Composable
private fun LogSection(logs: String, onClearLogs: () -> Unit) {
    val scrollState = rememberScrollState()
    LaunchedEffect(logs) { scrollState.animateScrollTo(scrollState.maxValue) }

    Spacer(modifier = Modifier.height(20.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("LOGS", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
        Text(
            text = "CLEAR",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable(onClick = onClearLogs)
                .padding(end = 2.dp),
        )
    }

    Card(modifier = Modifier.height(300.dp)) {
        SelectionContainer {
            Text(
                text = logs,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .verticalScroll(scrollState),
            )
        }
    }
}

@Composable
private fun ProcessingOverlay(onCancel: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x80000000))
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 25.dp),
                shape = RoundedCornerShape(7.dp),
            ) {
                Text("Cancel")
            }
        }
    }
}

/** A rounded, subtly tinted surface used to group form content and logs. */
@Composable
private fun Card(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(5.dp),
    ) {
        content()
    }
}
