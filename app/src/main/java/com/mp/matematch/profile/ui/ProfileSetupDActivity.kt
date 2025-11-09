package com.mp.matematch.profile.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.mp.matematch.databinding.ActivityProfileSetupDBinding
import com.mp.matematch.profile.viewmodel.ProfileViewModel

class ProfileSetupDActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileSetupDBinding
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileSetupDBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userType = intent.getStringExtra("USER_TYPE")

        // 🔹 기존 데이터 불러오기 (Firestore → UI)
        viewModel.user.observe(this) { user ->
            setSpinnerSelection(binding.spinnerAgeRange, user.prefAgeRange)
            setSpinnerSelection(binding.spinnerGenderPref, user.prefGender)
            setSpinnerSelection(binding.spinnerSleepPref, user.prefSleepSchedule)
            setSpinnerSelection(binding.spinnerSmokingPref, user.prefSmoking)
            setSpinnerSelection(binding.spinnerPetsPref, user.prefPets)
            setSpinnerSelection(binding.spinnerCleanPref, user.prefCleanliness)
        }

        // 🔹 뒤로가기 버튼
        binding.btnBack.setOnClickListener { finish() }

        // 🔹 다음 단계 이동 버튼
        binding.btnNext.setOnClickListener {
            saveIdealRoommateAndNext(userType)
        }
    }

    private fun saveIdealRoommateAndNext(userType: String?) {
        val ageRange = binding.spinnerAgeRange.selectedItem?.toString() ?: ""
        val genderPref = binding.spinnerGenderPref.selectedItem?.toString() ?: ""
        val sleepPref = binding.spinnerSleepPref.selectedItem?.toString() ?: ""
        val smokingPref = binding.spinnerSmokingPref.selectedItem?.toString() ?: ""
        val petsPref = binding.spinnerPetsPref.selectedItem?.toString() ?: ""
        val cleanPref = binding.spinnerCleanPref.selectedItem?.toString() ?: ""

        // ✅ ViewModel 업데이트
        viewModel.updateField("prefAgeRange", ageRange)
        viewModel.updateField("prefGender", genderPref)
        viewModel.updateField("prefSleepSchedule", sleepPref)
        viewModel.updateField("prefSmoking", smokingPref)
        viewModel.updateField("prefPets", petsPref)
        viewModel.updateField("prefCleanliness", cleanPref)

        // ✅ Firestore 저장
        viewModel.saveUserProfile { success ->
            if (success) {
                Snackbar.make(binding.root, "Ideal roommate 정보가 저장되었습니다.", Snackbar.LENGTH_SHORT).show()
                goToNextStep(userType)
            } else {
                Snackbar.make(binding.root, "저장 실패. 다시 시도해주세요.", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun goToNextStep(userType: String?) {
        val nextIntent = Intent(this, ProfileSetupEActivity::class.java)
        nextIntent.putExtra("USER_TYPE", userType)
        startActivity(nextIntent)
    }

    /** 🔹 Firestore 값과 Spinner의 text를 매칭하여 선택값으로 설정 */
    private fun setSpinnerSelection(spinner: android.widget.Spinner, value: String) {
        val adapter = spinner.adapter ?: return
        for (i in 0 until adapter.count) {
            if (adapter.getItem(i).toString() == value) {
                spinner.setSelection(i)
                break
            }
        }
    }
}
