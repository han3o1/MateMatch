package com.mp.matematch.profile.ui

import android.os.Bundle
import android.widget.CheckBox
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.mp.matematch.databinding.ActivityProfileSetupBBinding
import com.mp.matematch.profile.viewmodel.ProfileViewModel
import android.content.Intent
import com.mp.matematch.profile.ui.ProfileSetupCActivity

class ProfileSetupB2Activity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileSetupBBinding
    private val viewModel: ProfileViewModel by viewModels()

    private var selectedRoomType: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileSetupBBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userType = intent.getStringExtra("USER_TYPE")

        // 🔸 ViewModel 데이터 관찰
        viewModel.user.observe(this) { user ->
            // 기존 데이터 로드 시 UI에 반영
            binding.spinnerCity.setSelection(resources.getStringArray(com.mp.matematch.R.array.cities).indexOf(user.city))
            binding.spinnerDistrict.setSelection(resources.getStringArray(com.mp.matematch.R.array.districts).indexOf(user.district))
            binding.inputRent.setText(user.budgetMin.toString())
            binding.inputFee.setText(user.budgetMax.toString())
        }



        // 🔸 뒤로가기
        binding.btnBack.setOnClickListener { finish() }

        // 🔸 다음 버튼
        binding.btnNext.setOnClickListener {
            saveProfileAndNext(userType)
        }
    }

    private fun saveProfileAndNext(userType: String?) {
        val city = binding.spinnerCity.selectedItem?.toString() ?: ""
        val district = binding.spinnerDistrict.selectedItem?.toString() ?: ""
        val rent = binding.inputRent.text.toString().toIntOrNull() ?: 0
        val fee = binding.inputFee.text.toString().toIntOrNull() ?: 0

        // ✅ ViewModel에 반영
        viewModel.updateField("city", city)
        viewModel.updateField("district", district)
        viewModel.updateField("budgetMin", rent)
        viewModel.updateField("budgetMax", fee)
        viewModel.updateField("roomType", selectedRoomType)

        // ✅ Firestore 저장
        viewModel.saveUserProfile { success ->
            if (success) {
                Snackbar.make(binding.root, "저장 완료!", Snackbar.LENGTH_SHORT).show()
                goToNextStep(userType)
            } else {
                Snackbar.make(binding.root, "저장 실패. 다시 시도해주세요.", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun goToNextStep(userType: String?) {
        val nextIntent = Intent(this, ProfileSetupCActivity::class.java)
        nextIntent.putExtra("USER_TYPE", userType)
        startActivity(nextIntent)
    }

    // 🔧 확장함수: 특정 텍스트를 가진 버튼 리스트 반환
    private fun android.view.ViewGroup.findViewsWithText(vararg texts: String): List<com.google.android.material.button.MaterialButton> {
        val buttons = mutableListOf<com.google.android.material.button.MaterialButton>()
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is com.google.android.material.button.MaterialButton && texts.contains(child.text.toString())) {
                buttons.add(child)
            } else if (child is android.view.ViewGroup) {
                buttons.addAll(child.findViewsWithText(*texts))
            }
        }
        return buttons
    }
}
