package com.mp.matematch.profile.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import com.mp.matematch.databinding.ActivityProfileSetupEBinding
import com.mp.matematch.profile.viewmodel.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mp.matematch.main.ui.MainActivity


class ProfileSetupEActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileSetupEBinding
    private val viewModel: ProfileViewModel by viewModels()
    private var userType: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileSetupEBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userType = intent.getStringExtra("USER_TYPE")

        // ViewModel 데이터 관찰 (이전 단계 값 불러오기)
        viewModel.user.observe(this) { user ->
            binding.inputStatus.setText(user.statusMessage) // statusMessage -> inputStatus
            binding.inputIntro.setText(user.bio)            // bio -> inputIntro
        }

        // 뒤로가기
        binding.btnBack.setOnClickListener { finish() }

        // "Complete Profile" 버튼
        binding.btnComplete.setOnClickListener {
            saveFinalProfile()
        }
    }

    /** 데이터 저장 후 프로필 등록 **/
    private fun saveFinalProfile() {
        val status = binding.inputStatus.text?.toString()?.trim() ?: ""
        val intro = binding.inputIntro.text?.toString()?.trim() ?: ""

        // 필수 필드 확인
        if (intro.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Missing Required Fields")
                .setMessage("Please write a short self-introduction.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        // ViewModel 업데이트
        viewModel.updateField("statusMessage", status)
        viewModel.updateField("bio", intro)

        // Firestore에 최종 저장
        viewModel.saveUserProfile { success ->
            if (success) {
                Snackbar.make(binding.root, "Profile Updated!", Snackbar.LENGTH_LONG).show()
                goToMain(userType)
            } else {
                Snackbar.make(binding.root, "Profile Update Fail", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    /** MainActivity로 이동 **/
    private fun goToMain(userType: String?) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("USER_TYPE", userType)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finishAffinity()
    }
}