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

        // [1] “Find a Roommate” 선택 → 세부 선택 화면으로 이동
        binding.btnFindRoommate.setOnClickListener {
            val intent = Intent(this, SubPurposeSelectionActivity::class.java)
            startActivity(intent)
        }

        // [2] “Find a House” 선택 → 집 찾기 시나리오 바로 시작 (A → B → F)
        binding.btnFindHouse.setOnClickListener {
            val intent = Intent(this, ProfileSetupActivity::class.java).apply {
                putExtra("USER_TYPE", "HouseSeeker")
            }
            startActivity(intent)
        }
    }
}
