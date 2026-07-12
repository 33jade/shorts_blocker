package com.shortblocker

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.shortblocker.accessibility.ShortsBlockerService
import com.shortblocker.databinding.ActivityMainBinding
import com.shortblocker.settings.BlockSettingsRepository
import com.shortblocker.settings.RuntimeBlockSettings
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var settingsRepository: BlockSettingsRepository
    private var suppressSwitchCallback = false
    private var suppressAllowanceCallback = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        settingsRepository = BlockSettingsRepository(this)

        binding.blockingEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (suppressSwitchCallback) return@setOnCheckedChangeListener
            lifecycleScope.launch {
                settingsRepository.setBlockingEnabled(isChecked)
            }
        }

        val allowanceAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            DAILY_ALLOWANCE_OPTIONS.map(::formatAllowanceOption),
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.dailyAllowanceSpinner.adapter = allowanceAdapter
        binding.dailyAllowanceSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    if (suppressAllowanceCallback) return
                    lifecycleScope.launch {
                        settingsRepository.setDailyAllowanceMinutes(
                            DAILY_ALLOWANCE_OPTIONS[position],
                        )
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }

        binding.openAccessibilitySettingsButton.setOnClickListener {
            startActivity(Intent(this, AccessibilityConsentActivity::class.java))
        }

        binding.openPrivacyPolicyButton.setOnClickListener {
            startActivity(Intent(this, PrivacyPolicyActivity::class.java))
        }

        binding.temporaryUnblockCancelButton.setOnClickListener {
            cancelTemporaryUnblock()
        }

        lifecycleScope.launch {
            settingsRepository.runtimeSettingsFlow.collect(::renderSettings)
        }
    }

    override fun onResume() {
        super.onResume()
        renderServiceStatus()
    }

    private fun renderServiceStatus() {
        val enabled = isAccessibilityServiceEnabled()
        binding.serviceStatusValue.setText(
            if (enabled) {
                R.string.service_status_enabled
            } else {
                R.string.service_status_disabled
            },
        )
        binding.serviceStatusValue.setTextColor(
            getColor(
                if (enabled) {
                    R.color.status_enabled
                } else {
                    R.color.status_disabled
                },
            ),
        )
    }

    private fun renderSettings(settings: RuntimeBlockSettings) {
        suppressSwitchCallback = true
        binding.blockingEnabledSwitch.isChecked = settings.blockingEnabled
        suppressSwitchCallback = false

        val allowancePosition = DAILY_ALLOWANCE_OPTIONS.indexOf(settings.dailyAllowanceMinutes)
            .takeIf { it >= 0 }
            ?: DAILY_ALLOWANCE_OPTIONS.indexOf(DEFAULT_DAILY_ALLOWANCE_MINUTES)
        suppressAllowanceCallback = true
        if (binding.dailyAllowanceSpinner.selectedItemPosition != allowancePosition) {
            binding.dailyAllowanceSpinner.setSelection(allowancePosition)
        }
        suppressAllowanceCallback = false

        binding.consentStatusValue.setText(
            if (settings.isConsentAccepted()) {
                R.string.consent_status_accepted
            } else {
                R.string.consent_status_not_accepted
            },
        )

        val now = System.currentTimeMillis()
        val remainingTemporaryUnblockMs =
            (settings.temporaryUnblockUntilEpochMs - now).coerceAtLeast(0L)
        binding.temporaryUnblockStatusValue.text =
            if (remainingTemporaryUnblockMs > 0L) {
                getString(
                    R.string.temporary_unblock_active,
                    TimeUnit.MILLISECONDS.toMinutes(remainingTemporaryUnblockMs) + 1,
                )
            } else {
                getString(R.string.temporary_unblock_inactive)
            }
        binding.temporaryUnblockCancelButton.isEnabled = remainingTemporaryUnblockMs > 0L

        binding.dailyAllowanceStatusValue.text = getString(
            R.string.daily_allowance_status,
            TimeUnit.MILLISECONDS.toMinutes(settings.allowanceUsedMsFor(LocalDate.now())),
            TimeUnit.MILLISECONDS.toMinutes(settings.allowanceRemainingMs(LocalDate.now())),
        )
    }

    private fun cancelTemporaryUnblock() {
        lifecycleScope.launch {
            settingsRepository.setTemporaryUnblockUntil(0L)
        }
    }

    private fun formatAllowanceOption(minutes: Int): String =
        if (minutes == 0) {
            getString(R.string.allowance_not_allowed)
        } else {
            getString(R.string.allowance_minutes, minutes)
        }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponent = ComponentName(this, ShortsBlockerService::class.java)
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()

        return enabledServices
            .split(':')
            .mapNotNull(ComponentName::unflattenFromString)
            .any { it == expectedComponent }
    }

    private companion object {
        private val DAILY_ALLOWANCE_OPTIONS = listOf(0, 5, 10, 15, 20, 30, 60)
        private const val DEFAULT_DAILY_ALLOWANCE_MINUTES = 10
    }
}
