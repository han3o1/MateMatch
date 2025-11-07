package com.mp.matematch.profile.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mp.matematch.databinding.ActivityProfileSetupCBinding

class ProfileSetupCActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileSetupCBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileSetupCBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 뒤로가기
        binding.btnBack.setOnClickListener {
            finish()
        }

        // 다음 단계로 이동 (예: D단계)
        binding.btnNext.setOnClickListener {
            val intent = Intent(this, ProfileSetupDActivity::class.java)
            startActivity(intent)
        }
    }
}
