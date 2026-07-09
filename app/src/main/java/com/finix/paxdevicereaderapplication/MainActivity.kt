package com.finix.paxdevicereaderapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.finix.common.coreDeviceSdk.api.MerchantData
import com.finix.common.coreDeviceSdk.api.models.Environment
import com.finix.common.coreDeviceSdk.api.transaction.Country
import com.finix.paxdevicereaderapplication.device.TerminalDeviceFactory
import com.finix.paxdevicereaderapplication.ui.screen.TransactionsScreen
import com.finix.paxdevicereaderapplication.ui.theme.PaxDeviceReaderTheme
import com.finix.paxdevicereaderapplication.ui.transactions.TransactionsViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var terminalDeviceFactory: TerminalDeviceFactory

    /**
     * The SDK requires an [android.app.Activity] to create a terminal device, so it is built here
     * and handed to the ViewModel via assisted injection. Credentials are refreshed by the
     * ViewModel from persisted configuration on start.
     */
    private val device by lazy {
        terminalDeviceFactory.create(activity = this, merchantData = PLACEHOLDER_MERCHANT_DATA)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PaxDeviceReaderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val viewModel =
                        hiltViewModel<TransactionsViewModel, TransactionsViewModel.Factory> {
                            it.create(device)
                        }
                    TransactionsScreen(viewModel)
                }
            }
        }
    }

    private companion object {
        /** Empty credentials used only until the ViewModel loads the persisted configuration. */
        val PLACEHOLDER_MERCHANT_DATA = MerchantData(
            merchantId = "",
            mid = "",
            deviceId = "",
            applicationId = "",
            env = Environment.PROD,
            userId = "",
            password = "",
            country = Country.USA,
        )
    }
}
