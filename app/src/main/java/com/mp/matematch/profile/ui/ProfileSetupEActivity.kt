package com.mp.matematch.profile.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mp.matematch.databinding.ActivityProfileSetupEBinding

class ProfileSetupEActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileSetupEBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileSetupEBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 뒤로가기 버튼
        binding.btnBack.setOnClickListener { finish() }

        // 완료 버튼 클릭 시
        binding.btnComplete.setOnClickListener {
            val status = binding.inputStatus.text.toString().trim()
            val intro = binding.inputIntro.text.toString().trim()

            if (intro.isEmpty()) {
                Toast.makeText(this, "Please write a short self-introduction!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "Profile completed successfully!", Toast.LENGTH_SHORT).show()

            // TODO: Firebase 또는 Local DB 저장 가능
            finishAffinity() // 전체 액티비티 종료 (앱 홈으로 이동)
        }
    }
}
