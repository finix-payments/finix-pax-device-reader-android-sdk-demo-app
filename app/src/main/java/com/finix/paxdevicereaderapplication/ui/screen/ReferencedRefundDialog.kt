package com.finix.paxdevicereaderapplication.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.finix.common.coreDeviceSdk.api.transaction.ReferencedRefundRequest
import com.finix.paxdevicereaderapplication.domain.Money
import com.finix.paxdevicereaderapplication.domain.ReferencedRefundValidator

/**
 * Collects the transfer to refund and how much of it to refund.
 *
 * A referenced refund reverses a transfer that already went through, so the only inputs are
 * the original transfer's ID and an amount
 */
@Composable
fun ReferencedRefundDialog(
    initialAmount: String,
    onDismiss: () -> Unit,
    onConfirm: (ReferencedRefundRequest) -> Unit,
) {
    var transferId by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf(initialAmount) }
    var errors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Referenced Refund") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = "Refunds a transfer that has already been processed. " +
                            "No card is needed.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 12.dp),
                )

                OutlinedTextField(
                    value = transferId,
                    onValueChange = {
                        transferId = it.trim()
                        errors = errors - "transferId"
                    },
                    label = { Text("Transfer ID") },
                    placeholder = { Text("TRxxxxxxxxxxxxxxxxxxxxxx") },
                    singleLine = true,
                    isError = errors.containsKey("transferId"),
                    supportingText = errors["transferId"]?.let {
                        { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = Money.sanitizeInput(it, amount)
                        errors = errors - "amount"
                    },
                    label = { Text("Amount") },
                    placeholder = { Text("0.00") },
                    prefix = { Text("$") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = errors.containsKey("amount"),
                    supportingText = errors["amount"]?.let {
                        { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val request = ReferencedRefundRequest(
                        amount = Money.dollarsToCentsOrZero(amount),
                        transferId = transferId,
                    )
                    val result = ReferencedRefundValidator.validate(request)
                    if (result.isValid) onConfirm(request) else errors = result.errors
                },
            ) {
                Text("Refund")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
