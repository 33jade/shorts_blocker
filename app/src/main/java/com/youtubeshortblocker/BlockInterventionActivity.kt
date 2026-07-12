package com.youtubeshortblocker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.youtubeshortblocker.databinding.ActivityBlockInterventionBinding
import com.youtubeshortblocker.settings.BlockSettingsRepository
import com.youtubeshortblocker.settings.RuntimeBlockSettings
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class BlockInterventionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBlockInterventionBinding
    private lateinit var settingsRepository: BlockSettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlockInterventionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        settingsRepository = BlockSettingsRepository(this)

        binding.openYoutubeButton.setOnClickListener {
            openYoutube()
        }

        binding.openSettingsButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        binding.unblockFiveButton.setOnClickListener {
            temporarilyUnblockAndOpenYoutube(minutes = 5)
        }

        binding.unblockTenButton.setOnClickListener {
            temporarilyUnblockAndOpenYoutube(minutes = 10)
        }

        binding.cancelUnblockButton.setOnClickListener {
            cancelTemporaryUnblock()
        }

        lifecycleScope.launch {
            settingsRepository.runtimeSettingsFlow.collect(::renderSettings)
        }
    }

    private fun renderSettings(settings: RuntimeBlockSettings) {
        val today = LocalDate.now()
        binding.allowanceStatusValue.text = getString(
            R.string.block_intervention_allowance_status,
            TimeUnit.MILLISECONDS.toMinutes(settings.allowanceUsedMsFor(today)),
            TimeUnit.MILLISECONDS.toMinutes(settings.allowanceRemainingMs(today)),
        )
        binding.cancelUnblockButton.isEnabled =
            settings.isTemporarilyUnblocked(System.currentTimeMillis())
    }

    private fun temporarilyUnblockAndOpenYoutube(minutes: Long) {
        lifecycleScope.launch {
            val untilEpochMs = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(minutes)
            settingsRepository.setTemporaryUnblockUntil(untilEpochMs)
            openYoutube()
        }
    }

    private fun cancelTemporaryUnblock() {
        lifecycleScope.launch {
            settingsRepository.setTemporaryUnblockUntil(0L)
        }
    }

    private fun openYoutube() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(YOUTUBE_HOME_URL)).apply {
            setPackage(YOUTUBE_PACKAGE_NAME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        runCatching {
            startActivity(intent)
            finish()
        }.onFailure {
            Toast.makeText(this, R.string.youtube_home_open_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private companion object {
        private const val YOUTUBE_PACKAGE_NAME = "com.google.android.youtube"
        private const val YOUTUBE_HOME_URL = "https://www.youtube.com/"
    }
}
