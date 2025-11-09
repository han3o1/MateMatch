package com.mp.matematch.profile.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
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

        // 🔹 기존 사용자 데이터 관찰 (LiveData)
        viewModel.user.observe(this) { user ->
            // Firestore에서 불러온 데이터를 Spinner 기본 선택으로 반영
            setSpinnerSelection(binding.spinnerSleep, user.sleepSchedule)
            setSpinnerSelection(binding.spinnerSmoking, user.smoking)
            setSpinnerSelection(binding.spinnerPets, user.pets)
            setSpinnerSelection(binding.spinnerClean, user.cleanliness)
            setSpinnerSelection(binding.spinnerGuests, user.guestPolicy)
        }

        // 🔹 뒤로가기 버튼
        binding.btnBack.setOnClickListener { finish() }

        // 🔹 다음 버튼 클릭 → Firestore 저장 후 D단계 이동
        binding.btnNext.setOnClickListener {
            saveLifestyleAndNext(userType)
        }
    }

    private fun saveLifestyleAndNext(userType: String?) {
        val sleep = binding.spinnerSleep.selectedItem?.toString() ?: ""
        val smoking = binding.spinnerSmoking.selectedItem?.toString() ?: ""
        val pets = binding.spinnerPets.selectedItem?.toString() ?: ""
        val clean = binding.spinnerClean.selectedItem?.toString() ?: ""
        val guest = binding.spinnerGuests.selectedItem?.toString() ?: ""
        val social = getSocialPreferenceText()

        // ✅ ViewModel 업데이트
        viewModel.updateField("sleepSchedule", sleep)
        viewModel.updateField("smoking", smoking)
        viewModel.updateField("pets", pets)
        viewModel.updateField("cleanliness", clean)
        viewModel.updateField("guestPolicy", guest)
        viewModel.updateField("socialPreference", social)

        // ✅ Firestore 저장
        viewModel.saveUserProfile { success ->
            if (success) {
                Snackbar.make(binding.root, "Lifestyle 정보가 저장되었습니다.", Snackbar.LENGTH_SHORT).show()
                goToNextStep(userType)
            } else {
                Snackbar.make(binding.root, "저장 실패. 다시 시도해주세요.", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun goToNextStep(userType: String?) {
        val nextIntent = Intent(this, ProfileSetupDActivity::class.java)
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

    /** 🔹 Social Preference Spinner (XML 오타 보정용 처리)
     *  현재 spinner id 누락되어 있으므로 Spinner 객체를 findViewById로 접근 */
    private fun getSocialPreferenceText(): String {
        val spinnerSocial = findViewById<android.widget.Spinner>(com.mp.matematch.R.id.spinnerSocial)
        return spinnerSocial?.selectedItem?.toString() ?: ""
    }
}

