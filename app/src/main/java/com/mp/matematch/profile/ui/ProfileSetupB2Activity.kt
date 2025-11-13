package com.mp.matematch.profile.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.mp.matematch.R
import com.mp.matematch.databinding.ActivityProfileSetupB2Binding
import com.mp.matematch.profile.viewmodel.ProfileViewModel

class ProfileSetupB2Activity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileSetupB2Binding
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileSetupB2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        val userType = intent.getStringExtra("USER_TYPE")

        // ViewModel 데이터 관찰 (이전 단계 값 불러오기)
        viewModel.user.observe(this) { user ->
            binding.spinnerCity.setSelection(
                resources.getStringArray(R.array.cities).indexOf(user.city).coerceAtLeast(0)
            )
            binding.spinnerDistrict.setSelection(
                resources.getStringArray(R.array.districts).indexOf(user.district).coerceAtLeast(0)
            )
        }

        // 뒤로가기
        binding.btnBack.setOnClickListener { finish() }

        // 다음 버튼
        binding.btnNext.setOnClickListener {
            saveProfileAndNext(userType)
        }
    }

    /** 데이터 저장 후 다음 단계로 **/
    private fun saveProfileAndNext(userType: String?) {
        val city = binding.spinnerCity.selectedItem?.toString() ?: ""
        val district = binding.spinnerDistrict.selectedItem?.toString() ?: ""

        // 필수 필드 확인
        if (city.isEmpty() || district.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Missing Required Fields")
                .setMessage("Please select your preferred city and district.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        // ViewModel 업데이트
        viewModel.updateField("city", city)
        viewModel.updateField("district", district)

        viewModel.saveUserProfile { success ->
            if (success) {
                // 저장이 성공해야만 다음 단계로 이동
                goToNextStep(userType)
            } else {
                Toast.makeText(this, "Save failed. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** 다음 단계 Activity로 이동 **/
    private fun goToNextStep(userType: String?) {
        val nextIntent = Intent(this, ProfileSetupCActivity::class.java) // 👈 C (Lifestyle)로 이동
        nextIntent.putExtra("USER_TYPE", userType)
        startActivity(nextIntent)
    }
}