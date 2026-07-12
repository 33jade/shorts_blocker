package com.shortblocker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.shortblocker.databinding.ActivityPrivacyPolicyBinding

class PrivacyPolicyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPrivacyPolicyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrivacyPolicyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.closePrivacyPolicyButton.setOnClickListener {
            finish()
        }
    }
}
