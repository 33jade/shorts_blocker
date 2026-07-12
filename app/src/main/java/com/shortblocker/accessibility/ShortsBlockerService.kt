package com.shortblocker.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.shortblocker.BlockInterventionActivity
import com.shortblocker.BuildConfig
import com.shortblocker.settings.BlockSettingsRepository
import com.shortblocker.settings.RuntimeBlockSettings
import com.shortblocker.settings.ShortsAllowanceTracker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

class ShortsBlockerService : AccessibilityService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.Main.immediate)
    private val shortsDetector = ShortsDetector()
    private val nodeIdLogger = YoutubeNodeIdLogger(TAG)
    private val analysisEventController =
        AnalysisEventController(createAnalysisEventEnvironment(), ANALYSIS_INTERVAL_MS)
    private val settingsRepository by lazy { BlockSettingsRepository(this) }
    private val allowanceTracker = ShortsAllowanceTracker()

    private var evacuationJob: Job? = null
    private var analysisJob: Job? = null
    private var scheduledAnalysisJob: Job? = null
    private var lastBlockInterventionStartedElapsedMs = 0L

    private var settingsLoaded = false
    private var runtimeSettingsCache = RuntimeBlockSettings(
        blockingEnabled = false,
        acceptedConsentVersion = 0,
        dailyAllowanceMinutes = 0,
        allowanceUsedMs = 0L,
        allowanceDate = "",
        temporaryUnblockUntilEpochMs = 0L,
        blockInterventionScreenEnabled = true,
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        observeSettings()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString()
        val eventType = event.eventType

        serviceScope.launch {
            handleAccessibilityEvent(packageName, eventType)
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }

    private fun handleAccessibilityEvent(packageName: String?, eventType: Int) {
        if (!shouldHandleEvent(packageName, eventType)) return

        when (eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ->
                analysisEventController.request(AnalysisEventKind.WindowStateChanged)
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ->
                analysisEventController.request(AnalysisEventKind.WindowContentChanged)
        }
    }

    private fun shouldHandleEvent(packageName: String?, eventType: Int): Boolean {
        if (packageName != YOUTUBE_PACKAGE_NAME) return false
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            return false
        }
        if (!isAnalysisAllowed()) return false
        if (evacuationJob?.isActive == true) return false

        return true
    }

    private fun runAnalysis() {
        if (!isAnalysisAllowed() || evacuationJob?.isActive == true) return
        if (analysisJob?.isActive == true) {
            return
        }

        analysisJob = serviceScope.launch {
            try {
                val rootNode = rootInActiveWindow ?: return@launch
                val result = shortsDetector.detect(rootNode)
                if (result.isShortsScreen) {
                    if (recordAllowanceUsageIfAvailable()) return@launch

                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Shorts screen detected: ${result.reason}")
                        nodeIdLogger.log(rootNode, result.reason)
                    }
                    allowanceTracker.reset()
                    if (!showBlockInterventionScreen()) {
                        startEvacuationSequence()
                    }
                } else {
                    allowanceTracker.reset()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to analyze active window.", e)
            } finally {
                val currentJob = currentCoroutineContext()[Job]
                if (analysisJob == currentJob) {
                    analysisJob = null
                }
                analysisEventController.onAnalysisFinished()
            }
        }
    }

    private fun startEvacuationSequence() {
        if (!isRuntimeAllowed() || evacuationJob?.isActive == true) return

        val newJob = serviceScope.launch(start = CoroutineStart.LAZY) {
            try {
                EvacuationSequenceRunner(createEvacuationEnvironment()).run()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed during evacuation sequence.", e)
            } finally {
                resetSequenceStateIfCurrent(currentCoroutineContext()[Job])
            }
        }

        evacuationJob = newJob
        scheduledAnalysisJob?.cancel()
        newJob.start()
    }

    private fun createAnalysisEventEnvironment(): AnalysisEventController.Environment =
        object : AnalysisEventController.Environment {
            override fun elapsedRealtimeMs(): Long =
                SystemClock.elapsedRealtime()

            override fun startAnalysis() {
                runAnalysis()
            }

            override fun scheduleAnalysis(delayMs: Long) {
                scheduledAnalysisJob?.cancel()
                scheduledAnalysisJob = serviceScope.launch {
                    delay(delayMs)
                    analysisEventController.onScheduledAnalysisDue()
                }
            }

            override fun cancelScheduledAnalysis() {
                scheduledAnalysisJob?.cancel()
                scheduledAnalysisJob = null
            }
        }

    private fun observeSettings() {
        serviceScope.launch {
            settingsRepository.runtimeSettingsFlow
                .catch { cause ->
                    Log.e(TAG, "Failed to read block settings.", cause)
                    invalidateRuntimeSettings()
                }
                .collect { settings ->
                    settingsLoaded = true
                    runtimeSettingsCache = settings
                    if (!isAnalysisAllowed()) {
                        scheduledAnalysisJob?.cancel()
                        evacuationJob?.cancel()
                        allowanceTracker.reset()
                    }
                }
        }
    }

    private fun invalidateRuntimeSettings() {
        settingsLoaded = false
        runtimeSettingsCache = runtimeSettingsCache.copy(
            blockingEnabled = false,
            acceptedConsentVersion = 0,
        )
        scheduledAnalysisJob?.cancel()
        evacuationJob?.cancel()
        allowanceTracker.reset()
    }

    private fun createEvacuationEnvironment(): EvacuationSequenceRunner.Environment =
        object : EvacuationSequenceRunner.Environment {
            override fun isRuntimeAllowed(): Boolean =
                this@ShortsBlockerService.isRuntimeAllowed()

            override fun performGlobalAction(action: EvacuationAction): Boolean {
                val androidAction = when (action) {
                    EvacuationAction.Back -> GLOBAL_ACTION_BACK
                    EvacuationAction.Home -> GLOBAL_ACTION_HOME
                }
                val succeeded = performGlobalAction(androidAction)
                if (!succeeded && BuildConfig.DEBUG) {
                    Log.d(TAG, "Global action failed: $androidAction")
                }
                return succeeded
            }

            override fun isCurrentScreenShorts(): Boolean? {
                val rootNode: AccessibilityNodeInfo = rootInActiveWindow ?: return null
                return shortsDetector.detect(rootNode).isShortsScreen
            }

            override fun onHomeSucceeded() {
                if (runtimeSettingsCache.blockInterventionScreenEnabled) {
                    showBlockInterventionScreen()
                } else {
                    Toast.makeText(this@ShortsBlockerService, HOME_TOAST_TEXT, Toast.LENGTH_SHORT)
                        .show()
                }
            }

            override suspend fun delayAfterBack() {
                delay(REEVALUATION_INTERVAL_MS)
            }

            override suspend fun delayBeforeRootRetry() {
                delay(ROOT_RETRY_DELAY_MS)
            }
        }

    private fun resetSequenceStateIfCurrent(job: Job?) {
        if (evacuationJob == job) {
            evacuationJob = null
        }
    }

    private fun isRuntimeAllowed(): Boolean =
        settingsLoaded &&
            runtimeSettingsCache.isBlockingActive(
                nowEpochMs = System.currentTimeMillis(),
                currentDate = LocalDate.now(),
            )

    private fun isAnalysisAllowed(): Boolean =
        settingsLoaded && runtimeSettingsCache.isAnalysisAllowed(System.currentTimeMillis())

    private fun recordAllowanceUsageIfAvailable(): Boolean {
        val today = LocalDate.now()
        if (!runtimeSettingsCache.hasRemainingDailyAllowance(today)) {
            return false
        }

        val usageMs = allowanceTracker.recordShortsDetected(SystemClock.elapsedRealtime())
        if (usageMs > 0L) {
            serviceScope.launch {
                settingsRepository.recordAllowanceUsage(usageMs, today)
            }
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Shorts allowed by daily allowance. Recorded ${usageMs}ms.")
        }
        return true
    }

    private fun showBlockInterventionScreen(): Boolean {
        if (!runtimeSettingsCache.blockInterventionScreenEnabled) return false
        val nowElapsedMs = SystemClock.elapsedRealtime()
        if (nowElapsedMs - lastBlockInterventionStartedElapsedMs < BLOCK_INTERVENTION_COOLDOWN_MS) {
            return true
        }

        val intent = Intent(this, BlockInterventionActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        return runCatching {
            startActivity(intent)
            lastBlockInterventionStartedElapsedMs = nowElapsedMs
            true
        }.getOrElse { cause ->
            Log.e(TAG, "Failed to show block intervention screen.", cause)
            false
        }
    }

    companion object {
        private const val TAG = "ShortsBlockerService"
        private const val YOUTUBE_PACKAGE_NAME = "com.google.android.youtube"
        private const val ANALYSIS_INTERVAL_MS = 300L
        private const val REEVALUATION_INTERVAL_MS = 500L
        private const val ROOT_RETRY_DELAY_MS = 100L
        private const val BLOCK_INTERVENTION_COOLDOWN_MS = 1_500L
        private const val HOME_TOAST_TEXT = "Shortsから自動離脱しました"
    }
}
