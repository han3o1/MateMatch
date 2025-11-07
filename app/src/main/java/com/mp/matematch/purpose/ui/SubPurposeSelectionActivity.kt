package com.mp.matematch.purpose.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mp.matematch.databinding.ActivitySubPurposeSelectionBinding
import com.mp.matematch.profile.ui.ProfileSetupActivity

class SubPurposeSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySubPurposeSelectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubPurposeSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. "Yes, I have a place" 버튼 클릭 시
        binding.btnYesHavePlace.setOnClickListener {
            // 룸메찾기-집있 프로필 작성으로 이동
            val intent = Intent(this, ProfileSetupActivity::class.java).apply {
                putExtra("USER_TYPE", "Provider")
            }
            startActivity(intent)
        }

        // 2. "No, I don't have a place yet" 버튼 클릭 시
        binding.btnNoHavePlace.setOnClickListener {
            // 룸메찾기-집없 프로필 작성으로 이동
            val intent = Intent(this, ProfileSetupActivity::class.java).apply {
                putExtra("USER_TYPE", "Seeker")
            }
            startActivity(intent)
        }
    }
}