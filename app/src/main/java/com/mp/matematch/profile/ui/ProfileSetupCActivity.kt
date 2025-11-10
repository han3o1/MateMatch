package com.mp.matematch.profile.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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

        // ✅ 스피너 초기화
        setupSpinners()

        // 🔹 기존 사용자 데이터 관찰 (LiveData)
        viewModel.user.observe(this) { user ->
            // Firestore에서 불러온 데이터를 Spinner 기본 선택으로 반영
            setSpinnerSelection(binding.spinnerSleep, user.sleepSchedule)
            setSpinnerSelection(binding.spinnerSmoking, user.smoking)
            setSpinnerSelection(binding.spinnerPets, user.pets)
            setSpinnerSelection(binding.spinnerClean, user.cleanliness)
            setSpinnerSelection(binding.spinnerGuests, user.guestPolicy)
            setSpinnerSelection(binding.spinnerSocial, user.socialPreference)
        }

        // 🔹 뒤로가기 버튼
        binding.btnBack.setOnClickListener { finish() }

        // 🔹 다음 버튼 클릭 → Firestore 저장 후 D단계 이동
        binding.btnNext.setOnClickListener {
            saveLifestyleAndNext(userType)
        }
    }

    /** ✅ Spinner 세팅 함수 **/
    private fun setupSpinners() {
        // Sleep Schedule
        val sleepAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.select_sleep,   // ✅ 수정
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerSleep.adapter = sleepAdapter

        // Smoking
        val smokingAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.select_smoking,  // ✅ 수정
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerSmoking.adapter = smokingAdapter

        // Pets
        val petsAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.select_pets,  // ✅ 수정
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerPets.adapter = petsAdapter

        // Cleanliness
        val cleanAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.select_clean,  // ✅ 수정
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerClean.adapter = cleanAdapter

        // Guest Policy
        val guestAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.select_guest,  // ✅ 수정
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerGuests.adapter = guestAdapter

        // Social Preference
        val socialAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.select_social,  // ✅ 수정
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerSocial.adapter = socialAdapter
    }

    /** ✅ Spinner 현재 선택값 저장 + 다음 Activity로 이동 **/
    private fun saveLifestyleAndNext(userType: String?) {
        val sleep = binding.spinnerSleep.selectedItem?.toString() ?: ""
        val smoking = binding.spinnerSmoking.selectedItem?.toString() ?: ""
        val pets = binding.spinnerPets.selectedItem?.toString() ?: ""
        val clean = binding.spinnerClean.selectedItem?.toString() ?: ""
        val guest = binding.spinnerGuests.selectedItem?.toString() ?: ""
        val social = binding.spinnerSocial.selectedItem?.toString() ?: ""

        // ✅ 필수 필드 확인
        if (sleep.isEmpty() || smoking.isEmpty() || pets.isEmpty() || clean.isEmpty() ||
            guest.isEmpty() || social.isEmpty()
        ) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Missing Required Fields")
                .setMessage("Please fill in all required fields (marked with * ) before proceeding to the next step.")
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

        // Firestore 저장
        viewModel.saveUserProfile { success ->
            if (success) {
                Snackbar.make(binding.root, "Lifestyle 정보가 저장되었습니다.", Snackbar.LENGTH_SHORT).show()
                goToNextStep(userType)
            } else {
                Snackbar.make(binding.root, "저장 실패. 다시 시도해주세요.", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    /** ✅ 다음 Activity로 이동 **/
    private fun goToNextStep(userType: String?) {
        val nextIntent = Intent(this, ProfileSetupDActivity::class.java)
        nextIntent.putExtra("USER_TYPE", userType)
        startActivity(nextIntent)
    }

    /** ✅ Firestore 값과 Spinner 텍스트 매칭 **/
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


