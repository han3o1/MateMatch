package com.mp.matematch.profile.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mp.matematch.databinding.ActivityProfileSetupDBinding

class ProfileSetupDActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileSetupDBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileSetupDBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnNext.setOnClickListener {
            val intent = Intent(this, ProfileSetupEActivity::class.java)
            startActivity(intent)
        }
    }
}
