package com.mp.matematch.profile.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.mp.matematch.databinding.ActivityProfileSetupB2Binding  // ✅ B2 전용 바인딩으로 변경
import com.mp.matematch.profile.viewmodel.ProfileViewModel

class ProfileSetupB2Activity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileSetupB2Binding   // ✅ B2 전용
    private val viewModel: ProfileViewModel by viewModels()
    private var selectedRoomType: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileSetupB2Binding.inflate(layoutInflater)  // ✅ 올바른 XML inflate
        setContentView(binding.root)

        val userType = intent.getStringExtra("USER_TYPE")

        // 🔹 Firestore에서 불러온 기존 데이터 UI 반영
        viewModel.user.observe(this) { user ->
            val cities = resources.getStringArray(com.mp.matematch.R.array.cities)
            val districts = resources.getStringArray(com.mp.matematch.R.array.districts)

            binding.spinnerCity.setSelection(cities.indexOf(user.city))
            binding.spinnerDistrict.setSelection(districts.indexOf(user.district))

        }

        // 🔹 뒤로가기 버튼
        binding.btnBack.setOnClickListener { finish() }

        // 🔹 다음 버튼 → Firestore 저장 후 다음 단계로 이동
        binding.btnNext.setOnClickListener {
            saveProfileAndNext(userType)
        }
    }

    /** ✅ Firestore에 데이터 저장 후 다음 단계 이동 */
    private fun saveProfileAndNext(userType: String?) {
        val city = binding.spinnerCity.selectedItem?.toString() ?: ""
        val district = binding.spinnerDistrict.selectedItem?.toString() ?: ""


        // ✅ ViewModel 업데이트
        viewModel.updateField("city", city)
        viewModel.updateField("district", district)

        viewModel.updateField("roomType", selectedRoomType)

        // ✅ 필수 필드 검증
        if (city.isEmpty() || district.isEmpty()
        ) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Missing Required Fields")
                .setMessage("Please fill in all required fields (marked with * ) before proceeding to the next step.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

//        // ✅ Firestore 저장 후 다음 단계로 이동
//        viewModel.saveUserProfile { success ->
//            if (success) {
//                Snackbar.make(binding.root, "저장 완료!", Snackbar.LENGTH_SHORT).show()
//                goToNextStep(userType)
//            } else {
//                Snackbar.make(binding.root, "저장 실패. 다시 시도해주세요.", Snackbar.LENGTH_LONG).show()
//            }
//        }
    }

    /** ✅ 다음 Activity로 이동 */
    private fun goToNextStep(userType: String?) {
        val nextIntent = Intent(this, ProfileSetupCActivity::class.java)
        nextIntent.putExtra("USER_TYPE", userType)
        startActivity(nextIntent)
    }
}

