package com.mp.matematch.profile.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Spinner
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import com.mp.matematch.R
import com.mp.matematch.databinding.ActivityProfileSetupCBinding
import com.mp.matematch.profile.viewmodel.ProfileViewModel

class ProfileSetupCActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileSetupCBinding
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileSetupCBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userType = intent.getStringExtra("USER_TYPE")

        // 스피너 초기화
        setupSpinners()

        // ViewModel 데이터 관찰 (이전 단계 값 불러오기)
        viewModel.user.observe(this) { user ->
            // Firestore에서 불러온 데이터를 Spinner 기본 선택으로 반영
            setSpinnerSelection(binding.spinnerSleep, user.sleepSchedule)
            setSpinnerSelection(binding.spinnerSmoking, user.smoking)
            setSpinnerSelection(binding.spinnerPets, user.pets)
            setSpinnerSelection(binding.spinnerClean, user.cleanliness)
            setSpinnerSelection(binding.spinnerGuests, user.guestPolicy)
            setSpinnerSelection(binding.spinnerSocial, user.socialPreference)
        }

        // 뒤로가기
        binding.btnBack.setOnClickListener { finish() }

        // 다음 버튼
        binding.btnNext.setOnClickListener {
            saveLifestyleAndNext(userType)
        }
    }

    /**  Spinner 세팅 함수 **/
    private fun setupSpinners() {
        // Sleep Schedule
        val sleepItems = resources.getStringArray(R.array.select_sleep)
        val sleepAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, sleepItems)
        binding.spinnerSleep.setAdapter(sleepAdapter)

        // Smoking
        val smokingItems = resources.getStringArray(R.array.select_smoking)
        val smokingAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, smokingItems)
        binding.spinnerSmoking.setAdapter(smokingAdapter)

        // Pets
        val petsItems = resources.getStringArray(R.array.select_pets)
        val petsAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, petsItems)
        binding.spinnerPets.setAdapter(petsAdapter)

        // Cleanliness
        val cleanItems = resources.getStringArray(R.array.select_clean)
        val cleanAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, cleanItems)
        binding.spinnerClean.setAdapter(cleanAdapter)

        // Guest Policy
        val guestItems = resources.getStringArray(R.array.select_guest)
        val guestAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, guestItems)
        binding.spinnerGuests.setAdapter(guestAdapter)

        // Social Preference
        val socialItems = resources.getStringArray(R.array.select_social)
        val socialAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, socialItems)
        binding.spinnerSocial.setAdapter(socialAdapter)
    }

    /** 데이터 저장 후 다음 단계로 **/
    private fun saveLifestyleAndNext(userType: String?) {
        val sleep = binding.spinnerSleep.text.toString()
        val smoking = binding.spinnerSmoking.text.toString()
        val pets = binding.spinnerPets.text.toString()
        val clean = binding.spinnerClean.text.toString()
        val guest = binding.spinnerGuests.text.toString()
        val social = binding.spinnerSocial.text.toString()

        // 필수 필드 확인
        val sleepArray = resources.getStringArray(R.array.select_sleep)
        val smokingArray = resources.getStringArray(R.array.select_smoking)
        val petsArray = resources.getStringArray(R.array.select_pets)
        val cleanArray = resources.getStringArray(R.array.select_clean)
        val guestArray = resources.getStringArray(R.array.select_guest)
        val socialArray = resources.getStringArray(R.array.select_social)

        if (sleepArray.isEmpty() || smokingArray.isEmpty() || petsArray.isEmpty() ||
            cleanArray.isEmpty() || guestArray.isEmpty() || socialArray.isEmpty()
        ) {
            AlertDialog.Builder(this)
                .setTitle("Missing Required Fields")
                .setMessage("Please fill in all required fields before proceeding to the next step.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        // ViewModel 업데이트
        viewModel.updateField("sleepSchedule", sleep)
        viewModel.updateField("smoking", smoking)
        viewModel.updateField("pets", pets)
        viewModel.updateField("cleanliness", clean)
        viewModel.updateField("guestPolicy", guest)
        viewModel.updateField("socialPreference", social)

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
        val nextIntent = Intent(this, ProfileSetupDActivity::class.java)
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


