package com.mp.matematch.profile.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.mp.matematch.R
import com.mp.matematch.databinding.ActivityProfileSetupABinding
import com.mp.matematch.profile.model.User
import java.util.UUID

class ProfileSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileSetupABinding
    private var userType: String? = null
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private var selectedImageUri: Uri? = null

    companion object {
        private const val PICK_IMAGE_REQUEST = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileSetupABinding.inflate(layoutInflater)
        setContentView(binding.root)

        userType = intent.getStringExtra("USER_TYPE")

        /** ✅ 드롭다운 초기화 **/
        setupDropdowns()

        /** ✅ Move-in 날짜 선택기 **/
        binding.inputMoveInDate.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Move-in Date")
                .build()

            datePicker.addOnPositiveButtonClickListener {
                binding.inputMoveInDate.setText(datePicker.headerText)
            }
            datePicker.show(supportFragmentManager, "DATE_PICKER")
        }

        /** ✅ 이미지 업로드 버튼 **/
        binding.btnUploadPhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            startActivityForResult(
                Intent.createChooser(intent, "Select Profile Image"),
                PICK_IMAGE_REQUEST
            )
        }

        /** ✅ 다음 버튼 **/
        binding.btnNext.setOnClickListener {
            saveUserProfile()
        }

        /** ✅ 뒤로가기 **/
        binding.btnBack?.setOnClickListener { finish() }
    }

    /** ✅ AutoCompleteTextView 드롭다운 초기화 **/
    private fun setupDropdowns() {
        // ▫️ 나이
        val ageAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.ages,
            android.R.layout.simple_dropdown_item_1line
        )

        // ▫️ 성별
        val genderAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.genders,
            android.R.layout.simple_dropdown_item_1line
        )

        // ▫️ 직업
        val occupationAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.occupations,
            android.R.layout.simple_dropdown_item_1line
        )

        // ✅ 어댑터 연결
        binding.spinnerAge.setAdapter(ageAdapter)
        binding.spinnerGender.setAdapter(genderAdapter)
        binding.spinnerOccupation.setAdapter(occupationAdapter)

        // ✅ 클릭 시 자동 드롭다운 표시
        binding.spinnerAge.setOnClickListener { binding.spinnerAge.showDropDown() }
        binding.spinnerGender.setOnClickListener { binding.spinnerGender.showDropDown() }
        binding.spinnerOccupation.setOnClickListener { binding.spinnerOccupation.showDropDown() }

        // ✅ 디버깅용 로그
        binding.spinnerAge.setOnItemClickListener { parent, _, position, _ ->
            Log.d("ProfileSetup", "선택된 나이: ${parent.getItemAtPosition(position)}")
        }
        binding.spinnerGender.setOnItemClickListener { parent, _, position, _ ->
            Log.d("ProfileSetup", "선택된 성별: ${parent.getItemAtPosition(position)}")
        }
        binding.spinnerOccupation.setOnItemClickListener { parent, _, position, _ ->
            Log.d("ProfileSetup", "선택된 직업: ${parent.getItemAtPosition(position)}")
        }
    }

    /** ✅ onActivityResult: 이미지 선택 결과 처리 **/
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            binding.profileImage.setImageURI(selectedImageUri)
        }
    }

    /** ✅ Firestore + Storage 저장 함수 **/
    private fun saveUserProfile() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Log.e("ProfileSetup", "❌ FirebaseAuth user not found")
            return
        }

        // ✅ 입력값 추출
        val name = binding.inputName.text.toString().trim()
        val ageText = binding.spinnerAge.text.toString().trim()
        val occupation = binding.spinnerOccupation.text.toString().trim()
        val gender = binding.spinnerGender.text.toString().trim()
        val mbti = binding.inputMbti.text.toString().trim()
        val moveInDate = binding.inputMoveInDate.text.toString().trim()

        val age = ageText.filter { it.isDigit() }.toIntOrNull() ?: 0 // “23–26” 방지

        if (name.isEmpty()) {
            Log.e("ProfileSetup", "❌ 이름이 비어 있습니다.")
            return
        }

        // ✅ Firebase Storage 업로드 로직
        if (selectedImageUri != null) {
            val storageRef = storage.reference.child("profile_images/${UUID.randomUUID()}.jpg")
            val uploadTask = storageRef.putFile(selectedImageUri!!)

            uploadTask.addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    saveUserToFirestore(uid, name, age, gender, occupation, mbti, moveInDate, uri.toString())
                }
            }.addOnFailureListener { e ->
                Log.e("ProfileSetup", "❌ Image upload failed: ${e.message}")
                saveUserToFirestore(uid, name, age, gender, occupation, mbti, moveInDate, "")
            }
        } else {
            saveUserToFirestore(uid, name, age, gender, occupation, mbti, moveInDate, "")
        }
    }

    /** ✅ Firestore 저장 **/
    private fun saveUserToFirestore(
        uid: String,
        name: String,
        age: Int,
        gender: String,
        occupation: String,
        mbti: String,
        moveInDate: String,
        imageUrl: String
    ) {
        val user = User(
            uid = uid,
            userType = userType ?: "",
            name = name,
            age = age,
            gender = gender,
            occupation = occupation,
            mbti = mbti,
            moveInDate = moveInDate,
            profileImageUrl = imageUrl,

            // 이후 단계에서 채워질 필드
            city = "",
            district = "",
            addressDetail = "",
            budgetMin = 0,
            budgetMax = 0,
            roomType = "",
            duration = "",
            sleepSchedule = "",
            smoking = "",
            pets = "",
            cleanliness = "",
            guestPolicy = "",
            socialPreference = "",
            prefAgeRange = "",
            prefGender = "",
            prefSleepSchedule = "",
            prefSmoking = "",
            prefPets = "",
            prefCleanliness = "",
            bio = "",
            tags = emptyList()
        )

        db.collection("users").document(uid)
            .set(user)
            .addOnSuccessListener {
                Log.d("ProfileSetup", "✅ Firestore 저장 성공")
                goToNextStep()
            }
            .addOnFailureListener { e ->
                Log.e("ProfileSetup", "❌ Firestore 저장 실패: ${e.message}")
            }
    }

    /** ✅ 다음 Activity로 분기 **/
    private fun goToNextStep() {
        val nextActivity = when (userType) {
            "Provider" -> ProfileSetupBActivity::class.java
            "Seeker" -> ProfileSetupB2Activity::class.java
            "Finder" -> ProfileSetupB3Activity::class.java
            else -> null
        }

        nextActivity?.let {
            val intent = Intent(this, it)
            intent.putExtra("USER_TYPE", userType)
            startActivity(intent)
        }
    }
}
