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

        // [1] 집이 있는 사람 (Provider)
        binding.btnYesHavePlace.setOnClickListener {
            val intent = Intent(this, ProfileSetupActivity::class.java).apply {
                putExtra("USER_TYPE", "Provider") // A→B→C→D→E
            }
            startActivity(intent)
        }

        // [2] 집이 없는 사람 (Seeker)
        binding.btnNoHavePlace.setOnClickListener {
            val intent = Intent(this, ProfileSetupActivity::class.java).apply {
                putExtra("USER_TYPE", "Seeker") // A→B2->C→D→E
            }
            startActivity(intent)
        }
    }
}
