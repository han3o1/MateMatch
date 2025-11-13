package com.mp.matematch.profile.ui

import android.util.Log
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mp.matematch.R
import com.mp.matematch.databinding.ActivityProfileSetupB1Binding
import com.mp.matematch.profile.viewmodel.ProfileViewModel

class ProfileSetupB1Activity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileSetupB1Binding
    private val viewModel: ProfileViewModel by viewModels()
    private var selectedBuildingType: String = ""
    private var regionMap: Map<String, List<String>> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileSetupB1Binding.inflate(layoutInflater)
        setContentView(binding.root)

        val userType = intent.getStringExtra("USER_TYPE")

        // ViewModel 데이터 관찰 (이전 단계 값 불러오기)
        viewModel.user.observe(this) { user ->
            binding.spinnerCity.setText(user.city, false)
            updateDistrictSpinner(user.city)
            binding.spinnerDistrict.setText(user.district, false)
        }

        loadRegionsAndSetupSpinners()
        setupBuildingTypeButtons()

        // 뒤로가기
        binding.btnBack.setOnClickListener { finish() }

        // 다음 버튼
        binding.btnNext.setOnClickListener {
            saveProfileAndNext(userType)
        }
    }

    /** assets/regions.json을 읽고 스피너 설정 **/
    private fun loadRegionsAndSetupSpinners() {
        // JSON 파일 읽기
        val jsonString: String = try {
            assets.open("regions.json").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.e("ProfileSetupB1", "Error reading regions.json", e)
            return
        }

        // Gson으로 JSON을 Map<String, List<String>>으로 변환
        val mapType = object : TypeToken<Map<String, List<String>>>() {}.type
        regionMap = Gson().fromJson(jsonString, mapType)

        // city 스피너 설정
        val cities = regionMap.keys.toList().sorted()
        val cityAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, cities)
        binding.spinnerCity.setAdapter(cityAdapter)

        // district 스피너 초기 설정 (기본값)
        val defaultDistrictAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line,
            listOf("Select a city or province first"))
        binding.spinnerDistrict.setAdapter(defaultDistrictAdapter)

        // city 스피너 연동 리스너
        binding.spinnerCity.setOnItemClickListener { parent, view, position, id ->
            val selectedCity = parent.getItemAtPosition(position).toString()
            binding.spinnerDistrict.setText("", false)
            updateDistrictSpinner(selectedCity)
        }
    }

    /** 선택된 도시에 맞게 District 스피너의 어댑터 교체 **/
    private fun updateDistrictSpinner(selectedCity: String) {
        val districts = regionMap[selectedCity] ?: listOf("도시를 선택하세요")
        val newDistrictAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, districts)
        binding.spinnerDistrict.setAdapter(newDistrictAdapter)
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
        val city = binding.spinnerCity.text.toString().trim()
        val district = binding.spinnerDistrict.text.toString().trim()
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
        if (city.isEmpty() || district.isEmpty() ||
            selectedBuildingType.isEmpty() || rentText.isEmpty() || feeText.isEmpty()
        ) {
            AlertDialog.Builder(this)
                .setTitle("Missing Required Fields")
                .setMessage("Please fill in all required fields before proceeding to the next step.")
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