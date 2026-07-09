package com.finix.paxdevicereaderapplication.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.finix.paxdevicereaderapplication.domain.Money

/** Text field colors shared by the inline form fields, with no visible indicator line. */
@Composable
private fun borderlessFieldColors() = TextFieldDefaults.colors(
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    errorIndicatorColor = Color.Transparent,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
)

/**
 * A label-on-the-left, field-on-the-right row for entering a dollar amount. Input is filtered to a
 * valid partial amount as the user types.
 */
@Composable
fun AmountField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LabeledRow(label = label, modifier = modifier) {
        TextField(
            value = value,
            onValueChange = { onValueChange(Money.sanitizeInput(it, value)) },
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RectangleShape),
            singleLine = true,
            prefix = { Text("$") },
            placeholder = { Text("0") },
            colors = borderlessFieldColors(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done,
            ),
        )
    }
}

/** A label-on-the-left, field-on-the-right row for free-form text. */
@Composable
fun LabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Optional",
) {
    LabeledRow(label = label, modifier = modifier) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RectangleShape),
            singleLine = true,
            placeholder = { Text(placeholder) },
            colors = borderlessFieldColors(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done,
            ),
        )
    }
}

@Composable
private fun LabeledRow(
    label: String,
    modifier: Modifier = Modifier,
    field: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, modifier = Modifier.padding(end = 8.dp))
        Box(modifier = Modifier.weight(1f)) { field() }
    }
}
