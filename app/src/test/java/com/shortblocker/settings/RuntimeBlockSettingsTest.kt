package com.shortblocker.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RuntimeBlockSettingsTest {

    private val today: LocalDate = LocalDate.of(2026, 7, 12)

    @Test
    fun hasRemainingDailyAllowanceWhenUsedTimeIsBelowLimit() {
        val settings = settings(
            dailyAllowanceMinutes = 10,
            allowanceUsedMs = 9 * 60_000L,
            allowanceDate = today.toString(),
        )

        assertTrue(settings.hasRemainingDailyAllowance(today))
        assertFalse(settings.isBlockingActive(nowEpochMs = 1_000L, currentDate = today))
    }

    @Test
    fun blocksWhenDailyAllowanceIsFullyUsed() {
        val settings = settings(
            dailyAllowanceMinutes = 10,
            allowanceUsedMs = 10 * 60_000L,
            allowanceDate = today.toString(),
        )

        assertFalse(settings.hasRemainingDailyAllowance(today))
        assertTrue(settings.isBlockingActive(nowEpochMs = 1_000L, currentDate = today))
    }

    @Test
    fun treatsDifferentAllowanceDateAsFreshDay() {
        val settings = settings(
            dailyAllowanceMinutes = 10,
            allowanceUsedMs = 10 * 60_000L,
            allowanceDate = today.minusDays(1).toString(),
        )

        assertEquals(0L, settings.allowanceUsedMsFor(today))
        assertEquals(10 * 60_000L, settings.allowanceRemainingMs(today))
        assertTrue(settings.hasRemainingDailyAllowance(today))
    }

    @Test
    fun temporaryUnblockDisablesAnalysisAndBlocking() {
        val settings = settings(temporaryUnblockUntilEpochMs = 2_000L)

        assertFalse(settings.isAnalysisAllowed(nowEpochMs = 1_000L))
        assertFalse(settings.isBlockingActive(nowEpochMs = 1_000L, currentDate = today))
    }


    @Test
    fun schedulesAnalysisResumeWhenTemporaryUnblockIsActive() {
        val settings = settings(temporaryUnblockUntilEpochMs = 2_500L)

        assertEquals(1_500L, settings.analysisResumeDelayMs(nowEpochMs = 1_000L))
    }

    @Test
    fun doesNotScheduleAnalysisResumeWhenTemporaryUnblockExpired() {
        val settings = settings(temporaryUnblockUntilEpochMs = 1_000L)

        assertEquals(null, settings.analysisResumeDelayMs(nowEpochMs = 1_000L))
    }

    private fun settings(
        blockingEnabled: Boolean = true,
        acceptedConsentVersion: Int = BlockSettingsRepository.REQUIRED_CONSENT_VERSION,
        dailyAllowanceMinutes: Int = 0,
        allowanceUsedMs: Long = 0L,
        allowanceDate: String = today.toString(),
        temporaryUnblockUntilEpochMs: Long = 0L,
    ): RuntimeBlockSettings =
        RuntimeBlockSettings(
            blockingEnabled = blockingEnabled,
            acceptedConsentVersion = acceptedConsentVersion,
            dailyAllowanceMinutes = dailyAllowanceMinutes,
            allowanceUsedMs = allowanceUsedMs,
            allowanceDate = allowanceDate,
            temporaryUnblockUntilEpochMs = temporaryUnblockUntilEpochMs,
            blockInterventionScreenEnabled = true,
        )
}

