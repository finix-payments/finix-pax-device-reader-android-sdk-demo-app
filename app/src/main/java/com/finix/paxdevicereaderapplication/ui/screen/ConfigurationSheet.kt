package com.finix.paxdevicereaderapplication.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.finix.common.coreDeviceSdk.api.MerchantData
import com.finix.common.coreDeviceSdk.api.models.Environment
import com.finix.paxdevicereaderapplication.R
import com.finix.paxdevicereaderapplication.domain.MerchantConfigValidator
import com.finix.paxdevicereaderapplication.ui.transactions.TransactionsViewModel

/**
 * Bottom sheet for editing merchant credentials per environment. Credentials for every available
 * environment are loaded up front, so switching tabs is instant; only the selected environment is
 * saved.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigurationSheet(
    viewModel: TransactionsViewModel,
    environments: List<Environment>,
    initialEnvironment: Environment,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedEnvironment by remember { mutableStateOf(initialEnvironment) }
    var configByEnvironment by remember { mutableStateOf<Map<Environment, MerchantData>>(emptyMap()) }
    var validationErrors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    LaunchedEffect(environments) {
        configByEnvironment = environments.associateWith { env -> viewModel.merchantDataFor(env) }
    }

    val editedData = configByEnvironment[selectedEnvironment] ?: return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            SheetHeader(
                title = "Configuration",
                onCancel = onDismiss,
                onSave = {
                    val validation = MerchantConfigValidator.validate(editedData)
                    if (validation.isValid) {
                        viewModel.saveMerchantData(editedData.copy(env = selectedEnvironment))
                        onDismiss()
                    } else {
                        validationErrors = validation.errors
                    }
                },
            )

            Text("ENVIRONMENT", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(3.dp))

            TabRow(
                selectedTabIndex = environments.indexOf(selectedEnvironment).coerceAtLeast(0),
                modifier = Modifier.fillMaxWidth(),
            ) {
                environments.forEach { env ->
                    Tab(
                        selected = selectedEnvironment == env,
                        onClick = {
                            selectedEnvironment = env
                            validationErrors = emptyMap()
                        },
                        text = { Text(env.name) },
                    )
                }
            }

            MerchantDataForm(
                merchantData = editedData,
                validationErrors = validationErrors,
                onChange = { updated ->
                    configByEnvironment = configByEnvironment + (selectedEnvironment to updated)
                    validationErrors = emptyMap()
                },
            )
        }
    }
}

@Composable
private fun MerchantDataForm(
    merchantData: MerchantData,
    validationErrors: Map<String, String>,
    onChange: (MerchantData) -> Unit,
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        FieldSection("DEVICE") {
            ConfigField(
                label = "ID",
                value = merchantData.deviceId,
                errorKey = "deviceId",
                validationErrors = validationErrors,
                onValueChange = { onChange(merchantData.copy(deviceId = it)) },
            )
        }

        FieldSection("APPLICATION") {
            ConfigField(
                label = "ID",
                value = merchantData.applicationId,
                errorKey = "applicationId",
                validationErrors = validationErrors,
                onValueChange = { onChange(merchantData.copy(applicationId = it)) },
            )
        }

        FieldSection("MERCHANT") {
            ConfigField(
                label = "ID",
                value = merchantData.merchantId,
                errorKey = "merchantId",
                validationErrors = validationErrors,
                onValueChange = { onChange(merchantData.copy(merchantId = it)) },
            )
            ConfigField(
                label = "MID",
                value = merchantData.mid,
                errorKey = "mid",
                validationErrors = validationErrors,
                onValueChange = { onChange(merchantData.copy(mid = it)) },
            )
        }

        FieldSection("API KEY") {
            ConfigField(
                label = "Username",
                value = merchantData.userId,
                errorKey = "userId",
                validationErrors = validationErrors,
                onValueChange = { onChange(merchantData.copy(userId = it)) },
            )
            ConfigField(
                label = "Password",
                value = merchantData.password,
                errorKey = "password",
                validationErrors = validationErrors,
                onValueChange = { onChange(merchantData.copy(password = it)) },
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            painter = painterResource(
                                if (passwordVisible) R.drawable.ic_visibility
                                else R.drawable.ic_visibility_off,
                            ),
                            contentDescription = if (passwordVisible) {
                                "Hide password"
                            } else {
                                "Show password"
                            },
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun FieldSection(title: String, content: @Composable () -> Unit) {
    Spacer(modifier = Modifier.height(16.dp))
    Text(title, style = MaterialTheme.typography.labelLarge)
    content()
}

@Composable
private fun ConfigField(
    label: String,
    value: String,
    errorKey: String,
    validationErrors: Map<String, String>,
    onValueChange: (String) -> Unit,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val error = validationErrors[errorKey]
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Cancel / title / Save header shared by the configuration and "Others" sheets. */
@Composable
fun SheetHeader(
    title: String,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onCancel) { Text("Cancel") }
        Spacer(modifier = Modifier.weight(1f))
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = onSave) { Text("Save") }
    }
}
