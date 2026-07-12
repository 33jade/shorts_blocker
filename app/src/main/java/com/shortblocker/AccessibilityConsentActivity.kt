package com.shortblocker

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.shortblocker.databinding.ActivityAccessibilityConsentBinding
import com.shortblocker.settings.BlockSettingsRepository
import kotlinx.coroutines.launch

class AccessibilityConsentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccessibilityConsentBinding
    private lateinit var settingsRepository: BlockSettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccessibilityConsentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        settingsRepository = BlockSettingsRepository(this)

        binding.acceptConsentButton.setOnClickListener {
            lifecycleScope.launch {
                settingsRepository.acceptConsent(BlockSettingsRepository.REQUIRED_CONSENT_VERSION)
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                finish()
            }
        }

        binding.declineConsentButton.setOnClickListener {
            finish()
        }
    }
}
