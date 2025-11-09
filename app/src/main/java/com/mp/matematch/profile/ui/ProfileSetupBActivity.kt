package com.mp.matematch.profile.ui

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.mp.matematch.R
import com.mp.matematch.databinding.ActivityProfileSetupBBinding
import com.mp.matematch.profile.viewmodel.ProfileViewModel

class ProfileSetupBActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileSetupBBinding
    private val viewModel: ProfileViewModel by viewModels()

    private var selectedBuildingType: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileSetupBBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userType = intent.getStringExtra("USER_TYPE")

        // ✅ ViewModel 데이터 관찰 (이전 단계 값 불러오기)
        viewModel.user.observe(this) { user ->
            binding.spinnerCity.setSelection(
                resources.getStringArray(R.array.cities).indexOf(user.city).coerceAtLeast(0)
            )
            binding.spinnerDistrict.setSelection(
                resources.getStringArray(R.array.districts).indexOf(user.district).coerceAtLeast(0)
            )
            binding.inputRent.setText(user.budgetMin.toString())
            binding.inputFee.setText(user.budgetMax.toString())
        }

        // ✅ 빌딩 타입 버튼 하나만 선택 가능하게 설정
        setupBuildingTypeButtons()

        // 🔸 뒤로가기
        binding.btnBack.setOnClickListener { finish() }

        // 🔸 다음 버튼
        binding.btnNext.setOnClickListener {
            saveProfileAndNext(userType)
        }
    }

    /** ✅ 빌딩 타입 버튼 하나만 선택 가능하게 설정 **/
    private fun setupBuildingTypeButtons() {
        val parentLayout = binding.layoutBuildingType
        val buildingButtons = mutableListOf<MaterialButton>()

        // 모든 MaterialButton을 layoutBuildingType 내부에서 찾아서 리스트에 추가
        for (i in 0 until parentLayout.childCount) {
            val row = parentLayout.getChildAt(i)
            if (row is LinearLayout) {
                for (j in 0 until row.childCount) {
                    val button = row.getChildAt(j)
                    if (button is MaterialButton) {
                        buildingButtons.add(button)
                    }
                }
            }
        }

        // 각 버튼 클릭 시 스타일 및 상태 변경
        buildingButtons.forEach { button ->
            button.setOnClickListener {
                // 전체 버튼 초기화
                buildingButtons.forEach {
                    it.isChecked = false
                    it.setBackgroundColor(getColor(android.R.color.transparent))
                    it.strokeColor = getColorStateList(R.color.ic_launcher_background)
                    it.setTextColor(getColor(R.color.ic_launcher_background))
                }

                // 클릭된 버튼만 활성화 스타일 적용
                button.isChecked = true
                button.setBackgroundColor(getColor(R.color.ic_launcher_background))
                button.strokeColor = getColorStateList(R.color.ic_launcher_background)
                button.setTextColor(getColor(android.R.color.white))

                selectedBuildingType = button.tag.toString()
            }
        }
    }

    /** ✅ 프로필 저장 후 다음 단계로 **/
    private fun saveProfileAndNext(userType: String?) {
        val city = binding.spinnerCity.selectedItem?.toString() ?: ""
        val district = binding.spinnerDistrict.selectedItem?.toString() ?: ""
        val rent = binding.inputRent.text.toString().toIntOrNull() ?: 0
        val fee = binding.inputFee.text.toString().toIntOrNull() ?: 0

        // ViewModel에 반영
        viewModel.updateField("city", city)
        viewModel.updateField("district", district)
        viewModel.updateField("budgetMin", rent)
        viewModel.updateField("budgetMax", fee)
        viewModel.updateField("roomType", selectedBuildingType)

        // Firestore 저장
        viewModel.saveUserProfile { success ->
            if (success) {
                Snackbar.make(binding.root, "저장 완료!", Snackbar.LENGTH_SHORT).show()
                goToNextStep(userType)
            } else {
                Snackbar.make(binding.root, "저장 실패. 다시 시도해주세요.", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    /** ✅ 다음 단계로 이동 **/
    private fun goToNextStep(userType: String?) {
        val nextIntent = Intent(this, ProfileSetupCActivity::class.java)
        nextIntent.putExtra("USER_TYPE", userType)
        startActivity(nextIntent)
    }
}
