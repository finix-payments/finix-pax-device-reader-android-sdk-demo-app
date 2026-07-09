package com.finix.paxdevicereaderapplication.data.local

import android.content.Context
import androidx.core.content.edit
import com.finix.common.coreDeviceSdk.api.models.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists per-environment merchant credentials, split transfers, and tags in [android.content.SharedPreferences].
 *
 * All keys are namespaced by [Environment] so switching environments never mixes credentials.
 * This class performs blocking I/O and is expected to be called from a background dispatcher by
 * the repository.
 */
@Singleton
class ConfigLocalDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) {
    private val prefs by lazy {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    private fun envKey(base: String, env: Environment) = "${base}_${env.name}"

    // --- Merchant credentials ---------------------------------------------------------------

    fun hasConfig(env: Environment): Boolean =
        !prefs.getString(envKey(KEY_DEVICE_ID, env), null).isNullOrEmpty()

    fun readConfig(env: Environment): MerchantConfigDto =
        MerchantConfigDto(
            deviceId = prefs.getString(envKey(KEY_DEVICE_ID, env), "").orEmpty(),
            applicationId = prefs.getString(envKey(KEY_APPLICATION_ID, env), "").orEmpty(),
            merchantId = prefs.getString(envKey(KEY_MERCHANT_ID, env), "").orEmpty(),
            mid = prefs.getString(envKey(KEY_MID, env), "").orEmpty(),
            userId = prefs.getString(envKey(KEY_USERNAME, env), "").orEmpty(),
            password = prefs.getString(envKey(KEY_PASSWORD, env), "").orEmpty(),
        )

    fun writeConfig(env: Environment, config: MerchantConfigDto) {
        prefs.edit {
            putString(envKey(KEY_DEVICE_ID, env), config.deviceId)
            putString(envKey(KEY_APPLICATION_ID, env), config.applicationId)
            putString(envKey(KEY_MERCHANT_ID, env), config.merchantId)
            putString(envKey(KEY_MID, env), config.mid)
            putString(envKey(KEY_USERNAME, env), config.userId)
            putString(envKey(KEY_PASSWORD, env), config.password)
        }
    }

    // --- Split transfers --------------------------------------------------------------------

    fun readSplitTransfers(env: Environment): List<SplitTransferDto> {
        val stored = prefs.getString(envKey(KEY_SPLIT_TRANSFERS, env), null)
        if (stored.isNullOrEmpty()) return emptyList()
        return runCatching { json.decodeFromString<List<SplitTransferDto>>(stored) }
            .getOrDefault(emptyList())
    }

    fun writeSplitTransfers(env: Environment, splits: List<SplitTransferDto>) {
        prefs.edit { putString(envKey(KEY_SPLIT_TRANSFERS, env), json.encodeToString(splits)) }
    }

    fun clearSplitTransfers(env: Environment) {
        prefs.edit { remove(envKey(KEY_SPLIT_TRANSFERS, env)) }
    }

    // --- Tags -------------------------------------------------------------------------------

    fun readTags(env: Environment): String =
        prefs.getString(envKey(KEY_TAGS, env), "").orEmpty()

    fun writeTags(env: Environment, tags: String) {
        prefs.edit { putString(envKey(KEY_TAGS, env), tags) }
    }

    // --- Current environment ----------------------------------------------------------------

    fun readCurrentEnvironment(): Environment =
        prefs.getString(KEY_CURRENT_ENVIRONMENT, null)
            ?.let { runCatching { Environment.valueOf(it) }.getOrNull() }
            ?: Environment.PROD

    fun writeCurrentEnvironment(env: Environment) {
        prefs.edit { putString(KEY_CURRENT_ENVIRONMENT, env.name) }
    }

    private companion object {
        const val PREF_NAME = "merchant_data"

        const val KEY_DEVICE_ID = "device_id"
        const val KEY_APPLICATION_ID = "application_id"
        const val KEY_MERCHANT_ID = "merchant_id"
        const val KEY_MID = "merchant_mid"
        const val KEY_USERNAME = "username"
        const val KEY_PASSWORD = "password"
        const val KEY_SPLIT_TRANSFERS = "split_merchants"
        const val KEY_TAGS = "tags"
        const val KEY_CURRENT_ENVIRONMENT = "environment"
    }
}
