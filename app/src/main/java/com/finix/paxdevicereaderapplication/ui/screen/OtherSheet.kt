package com.finix.paxdevicereaderapplication.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.finix.common.coreDeviceSdk.api.transaction.SplitTransfer
import com.finix.paxdevicereaderapplication.domain.Money
import com.finix.paxdevicereaderapplication.domain.SplitTransferValidator
import com.finix.paxdevicereaderapplication.domain.TagParser

private const val NEW_ITEM_INDEX = -1

/**
 * Bottom sheet for editing transaction-level tags and an optional list of split transfers.
 * The first split entry is pinned to the primary merchant, and its ID cannot be edited.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherSheet(
    initialTags: String,
    initialSplits: List<SplitTransfer>,
    primaryMerchantId: String,
    onSave: (tags: String, splits: List<SplitTransfer>) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var tags by remember { mutableStateOf(initialTags) }
    var splitEnabled by remember { mutableStateOf(initialSplits.isNotEmpty()) }
    val splits = remember {
        initialSplits.ifEmpty { listOf(SplitTransfer(primaryMerchantId, 0)) }
            .toMutableStateList()
    }

    var editingIndex by remember { mutableStateOf<Int?>(null) }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        contentWindowInsets = { WindowInsets(0) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(16.dp),
        ) {
            SheetHeader(
                title = "Others",
                onCancel = onDismiss,
                onSave = {
                    val toSave = if (splitEnabled) splits.toList() else emptyList()
                    onSave(tags, toSave)
                    onDismiss()
                },
            )

            TagsField(
                value = tags,
                onValueChange = { tags = it },
            )

            SplitToggle(
                enabled = splitEnabled,
                onToggle = { enabled ->
                    splitEnabled = enabled
                    if (enabled && splits.isEmpty()) {
                        splits.add(SplitTransfer(primaryMerchantId, 0))
                    }
                },
            )

            if (splitEnabled) {
                SplitTransferList(
                    splits = splits,
                    onAdd = { editingIndex = NEW_ITEM_INDEX },
                    onEdit = { editingIndex = it },
                    onDelete = { index ->
                        splits.removeAt(index)
                        if (splits.isEmpty()) splitEnabled = false
                    },
                )
            }
        }
    }

    editingIndex?.let { index ->
        val existing = splits.getOrNull(index) ?: SplitTransfer("", 0)
        SplitTransferDialog(
            initial = existing,
            merchantIdEditable = index != 0,
            onDismiss = { editingIndex = null },
            onSave = { updated ->
                if (index == NEW_ITEM_INDEX) splits.add(updated) else splits[index] = updated
                editingIndex = null
            },
        )
    }
}

@Composable
private fun TagsField(value: String, onValueChange: (String) -> Unit) {
    val hasError = value.isNotEmpty() && !TagParser.isValid(value)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Tags") },
        placeholder = { Text("key:value, key2:value2") },
        isError = hasError,
        supportingText = if (hasError) {
            { Text("Invalid format. Use key:value, key2:value2") }
        } else {
            null
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SplitToggle(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp),
    ) {
        Text("Split Transfer")
        Spacer(Modifier.width(5.dp))
        Checkbox(checked = enabled, onCheckedChange = onToggle)
    }
}

@Composable
private fun SplitTransferList(
    splits: List<SplitTransfer>,
    onAdd: () -> Unit,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxHeight(0.8f)
            .padding(vertical = 8.dp),
    ) {
        itemsIndexed(splits) { index, split ->
            SplitTransferRow(
                index = index,
                split = split,
                deletable = index != 0,
                onClick = { onEdit(index) },
                onDelete = { onDelete(index) },
            )
        }
        item {
            TextButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                Text("+ Add Split Transfer")
            }
        }
    }
}

@Composable
private fun SplitTransferRow(
    index: Int,
    split: SplitTransfer,
    deletable: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
            .clickable(onClick = onClick),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Merchant ${index + 1}",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium.copy(
                    textDecoration = TextDecoration.Underline,
                ),
            )
            if (deletable) {
                IconButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier
                        .padding(start = 3.dp)
                        .height(20.dp),
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete split transfer")
                }
            }
        }

        Text("Merchant ID: ${split.merchantId}")
        Text(
            buildString {
                append("Amount: ${Money.formatCents(split.amount)}")
                split.fee?.takeIf { it > 0 }?.let { append(", Fee: ${Money.formatCents(it)}") }
            },
        )
        if (!split.tags.isNullOrEmpty()) {
            Text("Tags: ${TagParser.format(split.tags)}")
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete Split Transfer") },
            text = { Text("Are you sure you want to delete this merchant?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        confirmDelete = false
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SplitTransferDialog(
    initial: SplitTransfer,
    merchantIdEditable: Boolean,
    onDismiss: () -> Unit,
    onSave: (SplitTransfer) -> Unit,
) {
    var draft by remember { mutableStateOf(initial) }
    var errors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Split Merchant") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                SplitTransferForm(
                    split = draft,
                    merchantIdEditable = merchantIdEditable,
                    errors = errors,
                    onChange = { draft = it },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val result = SplitTransferValidator.validate(draft)
                    if (result.isValid) onSave(draft) else errors = result.errors
                },
            ) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun SplitTransferForm(
    split: SplitTransfer,
    merchantIdEditable: Boolean,
    errors: Map<String, String>,
    onChange: (SplitTransfer) -> Unit,
) {
    var tagsInput by remember { mutableStateOf(TagParser.format(split.tags)) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
    ) {
        OutlinedTextField(
            value = split.merchantId,
            onValueChange = { onChange(split.copy(merchantId = it)) },
            label = { Text("Merchant ID") },
            enabled = merchantIdEditable,
            isError = errors.containsKey("merchantId"),
            supportingText = errors["merchantId"]?.let {
                { Text(it, color = MaterialTheme.colorScheme.error) }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CentsAmountField(
                label = "Amount",
                errorKey = "amount",
                valueInCents = split.amount.takeIf { it > 0 },
                errors = errors,
                onValueChange = { onChange(split.copy(amount = it ?: 0L)) },
                modifier = Modifier.weight(1f),
            )
            CentsAmountField(
                label = "Fee",
                errorKey = "fee",
                valueInCents = split.fee,
                errors = errors,
                onValueChange = { onChange(split.copy(fee = it)) },
                modifier = Modifier.weight(1f),
            )
        }

        OutlinedTextField(
            value = tagsInput,
            onValueChange = { input ->
                tagsInput = input
                onChange(split.copy(tags = TagParser.toMap(input)))
            },
            label = { Text("Tags") },
            placeholder = { Text("key:value") },
            isError = errors.containsKey("tags"),
            supportingText = errors["tags"]?.let {
                { Text(it, color = MaterialTheme.colorScheme.error) }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CentsAmountField(
    label: String,
    errorKey: String,
    valueInCents: Long?,
    errors: Map<String, String>,
    onValueChange: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember(valueInCents == null) {
        mutableStateOf(valueInCents?.let { Money.formatCents(it).removePrefix("$") }.orEmpty())
    }

    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            val sanitized = Money.sanitizeInput(input, text)
            text = sanitized
            onValueChange(Money.dollarsToCents(sanitized))
        },
        placeholder = { Text("0.00") },
        prefix = { Text("$") },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        isError = errors.containsKey(errorKey),
        supportingText = errors[errorKey]?.let {
            { Text(it, color = MaterialTheme.colorScheme.error) }
        },
        modifier = modifier,
    )
}
