package com.finix.paxdevicereaderapplication.data.local

import android.content.Context
import android.util.Log
import com.finix.common.coreDeviceSdk.api.models.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads the bundled `assets/merchant_config.json`, which provides per-environment default
 * credentials seeded on first launch. The file is keyed by [Environment] name, e.g.:
 *
 * ```json
 * { "PROD": { "deviceId": "DV...", ... }, "SB": { ... } }
 * ```
 */
@Singleton
class ConfigAssetDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) {
    private fun readRawJson(): String? =
        runCatching {
            context.assets.open(CONFIG_FILE_NAME).bufferedReader().use { it.readText() }
        }.onFailure {
            Log.e(TAG, "Unable to read $CONFIG_FILE_NAME", it)
        }.getOrNull()?.takeIf { it.isNotBlank() }

    /** The environments declared in the asset file, falling back to all known environments. */
    fun environments(): List<Environment> {
        val raw = readRawJson() ?: return DEFAULT_ENVIRONMENTS
        return runCatching {
            json.parseToJsonElement(raw).jsonObject.keys
                .mapNotNull { key -> runCatching { Environment.valueOf(key) }.getOrNull() }
        }.getOrNull()?.takeIf { it.isNotEmpty() } ?: DEFAULT_ENVIRONMENTS
    }

    /** The default config for [env] from the asset file, or `null` when absent. */
    fun defaultConfig(env: Environment): MerchantConfigDto? {
        val raw = readRawJson() ?: return null
        return runCatching {
            val byEnv = json.decodeFromString<Map<String, MerchantConfigDto>>(raw)
            byEnv[env.name]
        }.onFailure {
            Log.e(TAG, "Unable to parse default config for $env", it)
        }.getOrNull()
    }

    private companion object {
        const val TAG = "ConfigAssetDataSource"
        const val CONFIG_FILE_NAME = "merchant_config.json"
        val DEFAULT_ENVIRONMENTS = listOf(Environment.PROD, Environment.SB)
    }
}
