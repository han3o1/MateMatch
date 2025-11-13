package com.mp.matematch.profile.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.mp.matematch.R
import com.mp.matematch.databinding.ActivityProfileSetupB1Binding
import com.mp.matematch.profile.viewmodel.ProfileViewModel

class ProfileSetupB1Activity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileSetupB1Binding
    private val viewModel: ProfileViewModel by viewModels()

    private var selectedBuildingType: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileSetupB1Binding.inflate(layoutInflater)
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

        // 빌딩 타입 버튼 하나만 선택 가능하게 설정
        setupBuildingTypeButtons()

        // 뒤로가기
        binding.btnBack.setOnClickListener { finish() }

        // 다음 버튼
        binding.btnNext.setOnClickListener {
            saveProfileAndNext(userType)
        }
    }

    /** 빌딩 타입 버튼 하나만 선택 가능하게 설정 **/
    private fun setupBuildingTypeButtons() {
        val parentLayout = binding.layoutBuildingType
        val buildingButtons = mutableListOf<com.google.android.material.button.MaterialButton>()

        // 모든 MaterialButton을 layoutBuildingType 내부에서 찾아서 리스트에 추가
        for (i in 0 until parentLayout.childCount) {
            val row = parentLayout.getChildAt(i)
            if (row is android.widget.LinearLayout) {
                for (j in 0 until row.childCount) {
                    val button = row.getChildAt(j)
                    if (button is com.google.android.material.button.MaterialButton) {
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
                    it.strokeColor = getColorStateList(R.color.brown_active)
                    it.setTextColor(getColor(R.color.brown_active))
                }

                // 클릭된 버튼만 활성화 스타일 적용
                button.isChecked = true
                button.setBackgroundColor(getColor(R.color.brown_active))
                button.strokeColor = getColorStateList(R.color.brown_active)
                button.setTextColor(getColor(android.R.color.white))

                selectedBuildingType = button.tag.toString()
            }
        }
    }

    /** 데이터 저장 후 다음 단계로 **/
    private fun saveProfileAndNext(userType: String?) {
        val cityArray = resources.getStringArray(R.array.cities)
        val districtArray = resources.getStringArray(R.array.districts)

        val city = binding.spinnerCity.selectedItem?.toString()?.trim() ?: ""
        val district = binding.spinnerDistrict.selectedItem?.toString()?.trim() ?: ""
        val rentText = binding.inputRent.text.toString().trim()
        val feeText = binding.inputFee.text.toString().trim()
        val rent = rentText.toIntOrNull() ?: 0
        val fee = feeText.toIntOrNull() ?: 0
        val selectedAmenities = mutableListOf<String>()

        if (binding.checkWiFi.isChecked) selectedAmenities.add("WiFi")
        if (binding.checkWasherDryer.isChecked) selectedAmenities.add("Washer/Dryer")
        if (binding.checkParking.isChecked) selectedAmenities.add("Parking")
        if (binding.checkGym.isChecked) selectedAmenities.add("Gym")
        if (binding.checkPool.isChecked) selectedAmenities.add("Pool")
        if (binding.checkAirConditioning.isChecked) selectedAmenities.add("Air Conditioning")
        if (binding.checkHeating.isChecked) selectedAmenities.add("Heating")
        if (binding.checkDishwasher.isChecked) selectedAmenities.add("Dishwasher")

        // 필수 필드 확인
        if (city == cityArray[0] || district == districtArray[0] ||
            selectedBuildingType.isEmpty() || rentText.isEmpty() || feeText.isEmpty()
        ) {
            AlertDialog.Builder(this)
                .setTitle("Missing Required Fields")
                .setMessage("Please fill in all required fields (marked with * ) before proceeding to the next step.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        // ViewModel 업데이트
        viewModel.updateField("city", city)
        viewModel.updateField("district", district)
        viewModel.updateField("buildingType", selectedBuildingType) // 'roomType' -> 'buildingType'
        viewModel.updateField("monthlyRent", rent)                  // 'budgetMin' -> 'monthlyRent'
        viewModel.updateField("maintenanceFee", fee)                // 'budgetMax' -> 'maintenanceFee'
        viewModel.updateField("amenities", selectedAmenities)

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
        val nextIntent = Intent(this, ProfileSetupCActivity::class.java)
        nextIntent.putExtra("USER_TYPE", userType)
        startActivity(nextIntent)
    }
}