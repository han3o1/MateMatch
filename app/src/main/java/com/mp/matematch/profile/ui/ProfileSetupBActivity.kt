package com.mp.matematch.profile.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mp.matematch.databinding.ActivityProfileSetupBBinding

class ProfileSetupBActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileSetupBBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileSetupBBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

//        binding.btnNext.setOnClickListener {
//            // TODO: Step 3 Activity로 이동
//        }
    }
}

