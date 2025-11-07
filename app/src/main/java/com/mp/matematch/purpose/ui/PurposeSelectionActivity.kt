package com.mp.matematch.purpose.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mp.matematch.databinding.ActivityPurposeSelectionBinding
import com.mp.matematch.profile.ui.ProfileSetupActivity

class PurposeSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPurposeSelectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPurposeSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. "Find a Roommate" 버튼 클릭 시
        binding.btnFindRoommate.setOnClickListener {
            // SubPurposeSelectionActivity로 이동
            val intent = Intent(this, SubPurposeSelectionActivity::class.java)
            startActivity(intent)
        }

        // 2. "Find a House" 버튼 클릭 시
        binding.btnFindHouse.setOnClickListener {
            // 집찾기 프로필 작성으로 이동
            val intent = Intent(this, ProfileSetupActivity::class.java)
            startActivity(intent)
        }
    }
}