package com.mp.matematch.profile.ui

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.mp.matematch.R
import com.mp.matematch.databinding.ActivityProfileSetupB3Binding  // ✅ B3 바인딩 사용
import com.mp.matematch.profile.viewmodel.ProfileViewModel

class ProfileSetupB3Activity : AppCompatActivity() {  // ✅ 클래스명 수정

    private lateinit var binding: ActivityProfileSetupB3Binding   // ✅ B3용 바인딩
    private val viewModel: ProfileViewModel by viewModels()

    private var selectedBuildingType: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileSetupB3Binding.inflate(layoutInflater) // ✅ 올바른 바인딩 inflate
        setContentView(binding.root)

        val userType = intent.getStringExtra("USER_TYPE")

        // ✅ Firestore에서 불러온 데이터 반영
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

        // ✅ 뒤로가기 버튼
        binding.btnBack.setOnClickListener { finish() }

        // ✅ 다음 버튼 클릭
        binding.btnNext.setOnClickListener {
            saveProfileAndNext(userType)
        }
    }

    /** ✅ 빌딩 타입 버튼 하나만 선택 가능하도록 설정 */
    private fun setupBuildingTypeButtons() {
        val parentLayout = binding.layoutBuildingType
        val buildingButtons = mutableListOf<MaterialButton>()

        // layoutBuildingType 내부의 모든 MaterialButton을 탐색
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

        // 클릭 시 스타일 변경 처리
        buildingButtons.forEach { button ->
            button.setOnClickListener {
                // 전체 버튼 기본화
                buildingButtons.forEach {
                    it.isChecked = false
                    it.setBackgroundColor(getColor(android.R.color.transparent))
                    it.strokeColor = getColorStateList(R.color.brown_500)
                    it.setTextColor(getColor(R.color.brown_500))
                }

                // 클릭된 버튼만 활성화
                button.isChecked = true
                button.setBackgroundColor(getColor(R.color.brown_500))
                button.strokeColor = getColorStateList(R.color.brown_500)
                button.setTextColor(getColor(android.R.color.white))

                selectedBuildingType = button.tag.toString()
            }
        }
    }

    /** ✅ 프로필 저장 후 다음 단계로 **/
    private fun saveProfileAndNext(userType: String?) {
        val city = binding.spinnerCity.selectedItem?.toString()?.trim() ?: ""
        val district = binding.spinnerDistrict.selectedItem?.toString()?.trim() ?: ""
        val rentText = binding.inputRent.text.toString().trim()
        val feeText = binding.inputFee.text.toString().trim()
        val rent = rentText.toIntOrNull() ?: 0
        val fee = feeText.toIntOrNull() ?: 0

        // ✅ 필수 필드 검증
        if (city.isEmpty() || district.isEmpty() || selectedBuildingType.isEmpty() ||
            rentText.isEmpty() || feeText.isEmpty()
        ) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Missing Required Fields")
                .setMessage("Please fill in all required fields (marked with * ) before proceeding to the next step.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        // ✅ ViewModel에 반영
        viewModel.updateField("city", city)
        viewModel.updateField("district", district)
        viewModel.updateField("budgetMin", rent)
        viewModel.updateField("budgetMax", fee)
        viewModel.updateField("roomType", selectedBuildingType)

//        // ✅ Firestore 저장
//        viewModel.saveUserProfile { success ->
//            if (success) {
//                androidx.appcompat.app.AlertDialog.Builder(this)
//                    .setTitle("Success")
//                    .setMessage("Your information has been saved successfully.")
//                    .setPositiveButton("Next") { _, _ ->
//                        goToNextStep(userType)
//                    }
//                    .show()
//            } else {
//                androidx.appcompat.app.AlertDialog.Builder(this)
//                    .setTitle("Save Failed")
//                    .setMessage("An error occurred while saving. Please try again.")
//                    .setPositiveButton("OK", null)
//                    .show()
//            }
//        }
    }

    /** ✅ 다음 단계로 이동 **/
    private fun goToNextStep(userType: String?) {
        val nextIntent = Intent(this, ProfileSetupCActivity::class.java)
        nextIntent.putExtra("USER_TYPE", userType)
        startActivity(nextIntent)
    }
}



