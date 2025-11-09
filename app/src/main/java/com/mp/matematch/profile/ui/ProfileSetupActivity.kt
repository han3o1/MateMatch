package com.mp.matematch.profile.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
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
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
            }
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
        val uid = auth.currentUser?.uid ?: return
        val name = binding.inputName.text.toString()
        val ageText = binding.spinnerAge.selectedItem?.toString() ?: ""
        val occupation = binding.spinnerOccupation.selectedItem?.toString() ?: ""
        val age = ageText.toIntOrNull() ?: 0
        val moveInDate = binding.inputMoveInDate.text.toString()

        // Firebase Storage에 이미지 업로드
        if (selectedImageUri != null) {
            val storageRef = storage.reference.child("profile_images/${UUID.randomUUID()}.jpg")
            val uploadTask = storageRef.putFile(selectedImageUri!!)

            uploadTask.addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    saveUserToFirestore(uid, name, age, occupation, moveInDate, uri.toString())
                }
            }.addOnFailureListener { e ->
                Log.e("ProfileSetup", "❌ Image upload failed: ${e.message}")
                saveUserToFirestore(uid, name, age, occupation, moveInDate, "")
            }
        } else {
            saveUserToFirestore(uid, name, age, occupation, moveInDate, "")
        }
    }

    /** ✅ Firestore 저장 **/
    private fun saveUserToFirestore(
        uid: String,
        name: String,
        age: Int,
        occupation: String,
        moveInDate: String,
        imageUrl: String
    ) {
        val user = User(
            uid = uid,
            userType = userType ?: "",
            name = name,
            age = age,
            occupation = occupation,
            moveInDate = moveInDate,
            profileImageUrl = imageUrl,

            // 기본값 초기화 (다음 단계에서 채워짐)
            gender = binding.spinnerGender.selectedItem?.toString() ?: "",
            mbti = binding.inputMbti.text.toString(),
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




