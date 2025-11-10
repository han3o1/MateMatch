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
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AlertDialog

class ProfileSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileSetupABinding
    private lateinit var userType: String   // ✅ 단 한 번만 선언
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

        // ✅ 인텐트로 전달된 userType 안전하게 저장
        userType = intent.getStringExtra("USER_TYPE") ?: "Unknown"
        Log.d("ProfileSetup", "🔸 Received USER_TYPE = $userType")

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

        /** ✅ 이미지 업로드 **/
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

    /** ✅ AutoCompleteTextView 드롭다운 설정 **/
    private fun setupDropdowns() {
        val ageAdapter = ArrayAdapter.createFromResource(this, R.array.ages, android.R.layout.simple_dropdown_item_1line)
        val genderAdapter = ArrayAdapter.createFromResource(this, R.array.genders, android.R.layout.simple_dropdown_item_1line)
        val occupationAdapter = ArrayAdapter.createFromResource(this, R.array.occupations, android.R.layout.simple_dropdown_item_1line)

        binding.spinnerAge.setAdapter(ageAdapter)
        binding.spinnerGender.setAdapter(genderAdapter)
        binding.spinnerOccupation.setAdapter(occupationAdapter)
    }

    /** ✅ 이미지 선택 처리 **/
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

        val age = ageText.filter { it.isDigit() }.toIntOrNull() ?: 0

        // ✅ 필수 입력값 확인
        if (name.isEmpty() || ageText.isEmpty() || gender.isEmpty() || occupation.isEmpty()|| moveInDate.isEmpty()) {
            // Snackbar 또는 AlertDialog로 알림 표시
            AlertDialog.Builder(this)
                .setTitle("Missing Required Fields")
                .setMessage("Please fill in all required fields (marked with * ) before proceeding to the next step.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        // ✅ Firebase Storage 업로드 로직
        if (selectedImageUri != null) {
            val storageRef = storage.reference.child("profile_images/${UUID.randomUUID()}.jpg")
            val uploadTask = storageRef.putFile(selectedImageUri!!)

            uploadTask.addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    saveUserToFirestore(
                        uid,
                        name,
                        age,
                        gender,
                        occupation,
                        mbti,
                        moveInDate,
                        uri.toString()
                    )
                }
            }.addOnFailureListener { e ->
                Log.e("ProfileSetup", "❌ Image upload failed: ${e.message}")
                saveUserToFirestore(uid, name, age, gender, occupation, mbti, moveInDate, "")
            }
        } else {
            saveUserToFirestore(uid, name, age, gender, occupation, mbti, moveInDate, "")
        }
    }


    /** ✅ Firestore 저장 함수 **/
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
            userType = userType,
            name = name,
            age = age,
            gender = gender,
            occupation = occupation,
            mbti = mbti,
            moveInDate = moveInDate,
            profileImageUrl = imageUrl,
            city = "", district = "", addressDetail = "",
            budgetMin = 0, budgetMax = 0, roomType = "",
            duration = "", sleepSchedule = "", smoking = "", pets = "",
            cleanliness = "", guestPolicy = "", socialPreference = "",
            prefAgeRange = "", prefGender = "", prefSleepSchedule = "",
            prefSmoking = "", prefPets = "", prefCleanliness = "",
            bio = "", tags = emptyList()
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

    /** ✅ 다음 단계 분기 **/
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
        } ?: Log.e("ProfileSetup", "❌ Unknown userType: $userType")
    }
}
