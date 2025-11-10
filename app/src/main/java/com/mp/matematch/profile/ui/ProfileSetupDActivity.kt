package com.mp.matematch.profile.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.mp.matematch.R
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

        // ✅ 스피너 초기화
        setupSpinners()

        // 🔹 Firestore → UI 반영
        viewModel.user.observe(this) { user ->
            setSpinnerSelection(binding.spinnerAgeRange, user.prefAgeRange)
            setSpinnerSelection(binding.spinnerGenderPref, user.prefGender)
            setSpinnerSelection(binding.spinnerSleepPref, user.prefSleepSchedule)
            setSpinnerSelection(binding.spinnerSmokingPref, user.prefSmoking)
            setSpinnerSelection(binding.spinnerPetsPref, user.prefPets)
            setSpinnerSelection(binding.spinnerCleanPref, user.prefCleanliness)
        }

        // 🔹 뒤로가기
        binding.btnBack.setOnClickListener { finish() }

        // 🔹 다음 버튼 → Firestore 저장 + E단계 이동
        binding.btnNext.setOnClickListener {
            saveIdealRoommateAndNext(userType)
        }
    }

    /** ✅ Spinner 초기화 */
    private fun setupSpinners() {
        // Age Range
        val ageAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.select_age_range,  // ✅ 배열명 수정 필요
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerAgeRange.adapter = ageAdapter

        // Gender Preference
        val genderAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.select_gender_pref,  // ✅ 배열명 수정 필요
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerGenderPref.adapter = genderAdapter

        // Sleep Schedule Preference
        val sleepAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.select_sleep,   // ✅ C단계와 동일하게 통일
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerSleepPref.adapter = sleepAdapter

        // Smoking Preference
        val smokingAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.select_smoking,  // ✅ C단계 동일
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerSmokingPref.adapter = smokingAdapter

        // Pets Preference
        val petsAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.select_pets,     // ✅ C단계 동일
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerPetsPref.adapter = petsAdapter

        // Cleanliness Preference
        val cleanAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.select_clean,    // ✅ C단계 동일
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerCleanPref.adapter = cleanAdapter
    }

    /** ✅ 저장 + 다음 단계 이동 */
    private fun saveIdealRoommateAndNext(userType: String?) {
        val ageRange = binding.spinnerAgeRange.selectedItem?.toString() ?: ""
        val genderPref = binding.spinnerGenderPref.selectedItem?.toString() ?: ""
        val sleepPref = binding.spinnerSleepPref.selectedItem?.toString() ?: ""
        val smokingPref = binding.spinnerSmokingPref.selectedItem?.toString() ?: ""
        val petsPref = binding.spinnerPetsPref.selectedItem?.toString() ?: ""
        val cleanPref = binding.spinnerCleanPref.selectedItem?.toString() ?: ""


        // ✅ 필수 필드 확인
        if (ageRange.isEmpty() || genderPref.isEmpty() || sleepPref.isEmpty() ||
            smokingPref.isEmpty() || petsPref.isEmpty() || cleanPref.isEmpty()
        ) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Missing Required Fields")
                .setMessage("Please fill in all required fields (marked with * ) before proceeding to the next step.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        // ViewModel에 반영
        viewModel.updateField("prefAgeRange", ageRange)
        viewModel.updateField("prefGender", genderPref)
        viewModel.updateField("prefSleepSchedule", sleepPref)
        viewModel.updateField("prefSmoking", smokingPref)
        viewModel.updateField("prefPets", petsPref)
        viewModel.updateField("prefCleanliness", cleanPref)

        // Firestore 저장
        viewModel.saveUserProfile { success ->
            if (success) {
                Snackbar.make(binding.root, "Ideal roommate 정보가 저장되었습니다.", Snackbar.LENGTH_SHORT).show()
                goToNextStep(userType)
            } else {
                Snackbar.make(binding.root, "저장 실패. 다시 시도해주세요.", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    /** ✅ 다음 Activity로 이동 */
    private fun goToNextStep(userType: String?) {
        val nextIntent = Intent(this, ProfileSetupEActivity::class.java)
        nextIntent.putExtra("USER_TYPE", userType)
        startActivity(nextIntent)
    }

    /** ✅ Firestore 값과 Spinner 텍스트 매칭 */
    private fun setSpinnerSelection(spinner: Spinner, value: String) {
        val adapter = spinner.adapter ?: return
        for (i in 0 until adapter.count) {
            if (adapter.getItem(i).toString() == value) {
                spinner.setSelection(i)
                break
            }
        }
    }
}
