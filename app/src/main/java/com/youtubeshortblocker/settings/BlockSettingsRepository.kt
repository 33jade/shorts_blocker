package com.youtubeshortblocker.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.blockSettingsDataStore by preferencesDataStore(name = "block_settings")

class BlockSettingsRepository(
    context: Context,
) {
    private val dataStore = context.applicationContext.blockSettingsDataStore

    val runtimeSettingsFlow: Flow<RuntimeBlockSettings> =
        dataStore.data.map { preferences ->
                RuntimeBlockSettings(
                    blockingEnabled = preferences[BLOCKING_ENABLED] ?: DEFAULT_BLOCKING_ENABLED,
                    acceptedConsentVersion = preferences[ACCEPTED_CONSENT_VERSION] ?: 0,
                dailyAllowanceMinutes = normalizeDailyAllowanceMinutes(
                    preferences[DAILY_ALLOWANCE_MINUTES] ?: DEFAULT_DAILY_ALLOWANCE_MINUTES,
                ),
                allowanceUsedMs = preferences[ALLOWANCE_USED_MS] ?: 0L,
                allowanceDate = preferences[ALLOWANCE_DATE].orEmpty(),
                temporaryUnblockUntilEpochMs = preferences[TEMPORARY_UNBLOCK_UNTIL_EPOCH_MS] ?: 0L,
                blockInterventionScreenEnabled =
                    preferences[BLOCK_INTERVENTION_SCREEN_ENABLED] ?: true,
            )
        }

    suspend fun setBlockingEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[BLOCKING_ENABLED] = enabled
        }
    }

    suspend fun setDailyAllowanceMinutes(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[DAILY_ALLOWANCE_MINUTES] = minutes
            if (preferences[ALLOWANCE_DATE].isNullOrBlank()) {
                preferences[ALLOWANCE_DATE] = LocalDate.now().toString()
            }
        }
    }

    suspend fun setTemporaryUnblockUntil(epochMs: Long) {
        dataStore.edit { preferences ->
            preferences[TEMPORARY_UNBLOCK_UNTIL_EPOCH_MS] = epochMs
        }
    }

    suspend fun acceptConsent(version: Int) {
        dataStore.edit { preferences ->
            preferences[ACCEPTED_CONSENT_VERSION] = version
        }
    }

    suspend fun recordAllowanceUsage(usageMs: Long, date: LocalDate) {
        if (usageMs <= 0L) return

        dataStore.edit { preferences ->
            val dateString = date.toString()
            val currentDate = preferences[ALLOWANCE_DATE]
            val currentUsedMs = if (currentDate == dateString) {
                preferences[ALLOWANCE_USED_MS] ?: 0L
            } else {
                0L
            }

            preferences[ALLOWANCE_DATE] = dateString
            preferences[ALLOWANCE_USED_MS] = currentUsedMs + usageMs
        }
    }

    companion object {
        const val REQUIRED_CONSENT_VERSION = 1
        private const val DEFAULT_BLOCKING_ENABLED = true
        private const val DEFAULT_DAILY_ALLOWANCE_MINUTES = 10
        private val ALLOWED_DAILY_ALLOWANCE_MINUTES = setOf(0, 5, 10, 15, 20, 30, 60)

        private val BLOCKING_ENABLED = booleanPreferencesKey("blocking_enabled")
        private val ACCEPTED_CONSENT_VERSION = intPreferencesKey("accepted_consent_version")
        private val DAILY_ALLOWANCE_MINUTES = intPreferencesKey("daily_allowance_minutes")
        private val ALLOWANCE_USED_MS = longPreferencesKey("allowance_used_ms")
        private val ALLOWANCE_DATE = stringPreferencesKey("allowance_date")
        private val TEMPORARY_UNBLOCK_UNTIL_EPOCH_MS =
            longPreferencesKey("temporary_unblock_until_epoch_ms")
        private val BLOCK_INTERVENTION_SCREEN_ENABLED =
            booleanPreferencesKey("block_intervention_screen_enabled")

        private fun normalizeDailyAllowanceMinutes(minutes: Int): Int =
            if (minutes in ALLOWED_DAILY_ALLOWANCE_MINUTES) {
                minutes
            } else {
                DEFAULT_DAILY_ALLOWANCE_MINUTES
            }
    }
}

data class RuntimeBlockSettings(
    val blockingEnabled: Boolean,
    val acceptedConsentVersion: Int,
    val dailyAllowanceMinutes: Int,
    val allowanceUsedMs: Long,
    val allowanceDate: String,
    val temporaryUnblockUntilEpochMs: Long,
    val blockInterventionScreenEnabled: Boolean,
) {
    fun isConsentAccepted(): Boolean =
        acceptedConsentVersion == BlockSettingsRepository.REQUIRED_CONSENT_VERSION

    fun isTemporarilyUnblocked(nowEpochMs: Long): Boolean =
        temporaryUnblockUntilEpochMs > nowEpochMs

    fun hasRemainingDailyAllowance(currentDate: LocalDate): Boolean {
        if (dailyAllowanceMinutes <= 0) return false
        val usedMs = allowanceUsedMsFor(currentDate)
        val allowanceMs = dailyAllowanceMinutes * 60_000L
        return usedMs < allowanceMs
    }

    fun allowanceUsedMsFor(currentDate: LocalDate): Long =
        if (allowanceDate == currentDate.toString()) {
            allowanceUsedMs
        } else {
            0L
        }

    fun allowanceRemainingMs(currentDate: LocalDate): Long {
        if (dailyAllowanceMinutes <= 0) return 0L
        val allowanceMs = dailyAllowanceMinutes * 60_000L
        return (allowanceMs - allowanceUsedMsFor(currentDate)).coerceAtLeast(0L)
    }

    fun isAnalysisAllowed(nowEpochMs: Long): Boolean =
        blockingEnabled &&
            isConsentAccepted() &&
            !isTemporarilyUnblocked(nowEpochMs)

    fun isBlockingActive(nowEpochMs: Long, currentDate: LocalDate): Boolean =
        blockingEnabled &&
            isConsentAccepted() &&
            !isTemporarilyUnblocked(nowEpochMs) &&
            !hasRemainingDailyAllowance(currentDate)
}
