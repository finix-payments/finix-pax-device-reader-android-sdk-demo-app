package com.finix.paxdevicereaderapplication.data.repository

import com.finix.common.coreDeviceSdk.api.MerchantData
import com.finix.common.coreDeviceSdk.api.models.Environment
import com.finix.common.coreDeviceSdk.api.transaction.SplitTransfer
import com.finix.paxdevicereaderapplication.data.local.ConfigAssetDataSource
import com.finix.paxdevicereaderapplication.data.local.ConfigLocalDataSource
import com.finix.paxdevicereaderapplication.data.local.MerchantConfigDto
import com.finix.paxdevicereaderapplication.data.local.SplitTransferDto
import com.finix.paxdevicereaderapplication.di.IoDispatcher
import com.finix.paxdevicereaderapplication.domain.MerchantConfigValidator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultConfigRepository @Inject constructor(
    private val local: ConfigLocalDataSource,
    private val assets: ConfigAssetDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ConfigRepository {

    override suspend fun environments(): List<Environment> = withContext(ioDispatcher) {
        assets.environments()
    }

    override suspend fun currentEnvironment(): Environment = withContext(ioDispatcher) {
        local.readCurrentEnvironment()
    }

    override suspend fun loadMerchantData(env: Environment): MerchantData =
        withContext(ioDispatcher) {
            if (!local.hasConfig(env)) {
                seedDefaultsIfValid(env)
            }
            local.readConfig(env).toMerchantData(env)
        }

    override suspend fun saveMerchantData(data: MerchantData) = withContext(ioDispatcher) {
        local.writeConfig(data.env, MerchantConfigDto.from(data))
        local.writeCurrentEnvironment(data.env)
    }

    override suspend fun loadSplitTransfers(env: Environment): List<SplitTransfer> =
        withContext(ioDispatcher) {
            local.readSplitTransfers(env).map { it.toDomain() }
        }

    override suspend fun saveSplitTransfers(env: Environment, splits: List<SplitTransfer>) =
        withContext(ioDispatcher) {
            local.writeSplitTransfers(env, splits.map { SplitTransferDto.from(it) })
        }

    override suspend fun clearSplitTransfers(env: Environment) = withContext(ioDispatcher) {
        local.clearSplitTransfers(env)
    }

    override suspend fun loadTags(env: Environment): String = withContext(ioDispatcher) {
        local.readTags(env)
    }

    override suspend fun saveTags(env: Environment, tags: String) = withContext(ioDispatcher) {
        local.writeTags(env, tags)
    }

    /** Seeds persisted credentials for [env] from the asset defaults, but only if they are valid. */
    private fun seedDefaultsIfValid(env: Environment) {
        val default = assets.defaultConfig(env) ?: return
        val merchantData = default.toMerchantData(env)
        if (MerchantConfigValidator.validate(merchantData).isValid) {
            local.writeConfig(env, default)
        }
    }
}
