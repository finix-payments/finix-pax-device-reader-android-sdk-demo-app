package com.finix.paxdevicereaderapplication.data.repository

import com.finix.common.coreDeviceSdk.api.MerchantData
import com.finix.common.coreDeviceSdk.api.models.Environment
import com.finix.common.coreDeviceSdk.api.transaction.SplitTransfer

/**
 * Single source of truth for merchant configuration, split transfers and tags.
 *
 * Implementations coordinate persisted values with the bundled asset defaults and are safe to call
 * from the main thread (I/O is dispatched internally). All reads are per-[Environment].
 */
interface ConfigRepository {

    /** Environments the app supports, as declared by the bundled asset config. */
    suspend fun environments(): List<Environment>

    /** The environment last selected by the user. */
    suspend fun currentEnvironment(): Environment

    /**
     * Loads the credentials for [env]. On first use for an environment, defaults are seeded from
     * the bundled asset config when present and valid.
     */
    suspend fun loadMerchantData(env: Environment): MerchantData

    /** Persists [data] and remembers its environment as the current one. */
    suspend fun saveMerchantData(data: MerchantData)

    suspend fun loadSplitTransfers(env: Environment): List<SplitTransfer>

    suspend fun saveSplitTransfers(env: Environment, splits: List<SplitTransfer>)

    suspend fun clearSplitTransfers(env: Environment)

    suspend fun loadTags(env: Environment): String

    suspend fun saveTags(env: Environment, tags: String)
}
