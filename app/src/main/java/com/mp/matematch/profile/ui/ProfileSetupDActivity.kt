package com.mp.matematch.profile.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
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

        // 스피너 초기화
        setupSpinners()

        // ViewModel 데이터 관찰 (이전 단계 값 불러오기)
        viewModel.user.observe(this) { user ->
            setSpinnerSelection(binding.spinnerAgeRange, user.prefAgeRange)
            setSpinnerSelection(binding.spinnerGenderPref, user.prefGender)
            setSpinnerSelection(binding.spinnerSleepPref, user.prefSleepSchedule)
            setSpinnerSelection(binding.spinnerSmokingPref, user.prefSmoking)
            setSpinnerSelection(binding.spinnerPetsPref, user.prefPets)
            setSpinnerSelection(binding.spinnerCleanPref, user.prefCleanliness)
        }

        // 뒤로가기
        binding.btnBack.setOnClickListener { finish() }

        // 다음 버튼
        binding.btnNext.setOnClickListener {
            saveIdealRoommateAndNext(userType)
        }
    }

    /**  Spinner 세팅 함수 **/
    private fun setupSpinners() {
        // Age Range
        val ageAdapter = ArrayAdapter.createFromResource(
            this, R.array.select_age_range, android.R.layout.simple_dropdown_item_1line
        )
        binding.spinnerAgeRange.setAdapter(ageAdapter)

        // Gender Preference
        val genderAdapter = ArrayAdapter.createFromResource(
            this, R.array.select_gender_pref, android.R.layout.simple_dropdown_item_1line
        )
        binding.spinnerGenderPref.setAdapter(genderAdapter)

        // Sleep Schedule Preference
        val sleepAdapter = ArrayAdapter.createFromResource(
            this, R.array.select_sleep_pref, android.R.layout.simple_dropdown_item_1line
        )
        binding.spinnerSleepPref.setAdapter(sleepAdapter)

        // Smoking Preference
        val smokingAdapter = ArrayAdapter.createFromResource(
            this, R.array.select_smoking_pref, android.R.layout.simple_dropdown_item_1line
        )
        binding.spinnerSmokingPref.setAdapter(smokingAdapter)

        // Pets Preference
        val petsAdapter = ArrayAdapter.createFromResource(
            this, R.array.select_pets_pref, android.R.layout.simple_dropdown_item_1line
        )
        binding.spinnerPetsPref.setAdapter(petsAdapter)

        // Cleanliness Preference
        val cleanAdapter = ArrayAdapter.createFromResource(
            this, R.array.select_clean_pref, android.R.layout.simple_dropdown_item_1line
        )
        binding.spinnerCleanPref.setAdapter(cleanAdapter)
    }

    /** 데이터 저장 후 다음 단계로 **/
    private fun saveIdealRoommateAndNext(userType: String?) {
        val ageRange = binding.spinnerAgeRange.text.toString()
        val genderPref = binding.spinnerGenderPref.text.toString()
        val sleepPref = binding.spinnerSleepPref.text.toString()
        val smokingPref = binding.spinnerSmokingPref.text.toString()
        val petsPref = binding.spinnerPetsPref.text.toString()
        val cleanPref = binding.spinnerCleanPref.text.toString()

        // 필수 필드 확인
        val ageArray = resources.getStringArray(R.array.select_age_range)
        val genderArray = resources.getStringArray(R.array.select_gender_pref)
        val sleepArray = resources.getStringArray(R.array.select_sleep_pref)
        val smokingArray = resources.getStringArray(R.array.select_smoking_pref)
        val petsArray = resources.getStringArray(R.array.select_pets_pref)
        val cleanArray = resources.getStringArray(R.array.select_clean_pref)

        if (ageArray.isEmpty() || genderArray.isEmpty() || sleepArray.isEmpty() ||
            smokingArray.isEmpty() || petsArray.isEmpty() || cleanArray.isEmpty()
        ) {
            AlertDialog.Builder(this)
                .setTitle("Missing Required Fields")
                .setMessage("Please select all ideal roommate preferences.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        // ViewModel 업데이트
        viewModel.updateField("prefAgeRange", ageRange)
        viewModel.updateField("prefGender", genderPref)
        viewModel.updateField("prefSleepSchedule", sleepPref)
        viewModel.updateField("prefSmoking", smokingPref)
        viewModel.updateField("prefPets", petsPref)
        viewModel.updateField("prefCleanliness", cleanPref)

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
        val nextIntent = Intent(this, ProfileSetupEActivity::class.java)
        nextIntent.putExtra("USER_TYPE", userType)
        startActivity(nextIntent)
    }

    /** Firestore 값과 Spinner 텍스트 매칭 **/
    private fun setSpinnerSelection(spinner: AutoCompleteTextView, value: String) {
        val adapter = spinner.adapter ?: return
        for (i in 0 until adapter.count) {
            if (adapter.getItem(i).toString() == value) {
                spinner.setText(adapter.getItem(i).toString(), false)
                break
            }
        }
    }
}
