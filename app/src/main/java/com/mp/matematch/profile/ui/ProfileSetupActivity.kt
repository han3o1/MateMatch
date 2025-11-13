package com.mp.matematch.profile.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.mp.matematch.R
import com.mp.matematch.databinding.ActivityProfileSetupABinding
import java.util.UUID
import androidx.appcompat.app.AlertDialog

class ProfileSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileSetupABinding
    private lateinit var userType: String
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private var selectedImageUri: Uri? = null

    // 3. 이미지 선택 결과 처리를 위한 Launcher 등록
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            binding.profileImage.setImageURI(selectedImageUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileSetupABinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 이전 Activity에서 전달받은 userType 저장
        userType = intent.getStringExtra("USER_TYPE") ?: "Unknown"
        Log.d("ProfileSetup", "Received USER_TYPE = $userType")

        // 드롭다운 메뉴 초기화
        setupDropdowns()

        // '입주 가능 날짜' EditText 클릭 시 날짜 선택기 표시
        binding.inputMoveInDate.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker().build()
            datePicker.addOnPositiveButtonClickListener {
                binding.inputMoveInDate.setText(datePicker.headerText)
            }
            datePicker.show(supportFragmentManager, "DATE_PICKER")
        }

        // '사진 업로드' 버튼 클릭 시 갤러리 열기
        binding.btnUploadPhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            // 4. ActivityResultLauncher 실행
            pickImageLauncher.launch(Intent.createChooser(intent, "Select Profile Image"))
        }

        // '다음' 버튼 클릭 시 유효성 검사 및 저장
        binding.btnNext.setOnClickListener {
            // 5. 저장 함수 호출
            validateAndSave()
        }

        // (XML에 btnBack이 있다는 가정 하에)
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupDropdowns() {
        val genderAdapter = ArrayAdapter.createFromResource(this, R.array.genders, android.R.layout.simple_dropdown_item_1line)
        val occupationAdapter = ArrayAdapter.createFromResource(this, R.array.occupations, android.R.layout.simple_dropdown_item_1line)
        binding.spinnerGender.setAdapter(genderAdapter)
        binding.spinnerOccupation.setAdapter(occupationAdapter)
    }

    /** 입력값 유효성 검사 **/
    private fun validateAndSave() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Log.e("ProfileSetup", "User not found")
            return
        }

        val name = binding.inputName.text.toString().trim()
        val ageText = binding.inputAge.text.toString().trim()
        val occupation = binding.spinnerOccupation.text.toString().trim()
        val gender = binding.spinnerGender.text.toString().trim()
        val moveInDate = binding.inputMoveInDate.text.toString().trim()

        // 필수 필드 확인
        if (name.isEmpty() || gender.isEmpty() || occupation.isEmpty() || moveInDate.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Missing Required Fields")
                .setMessage("Please fill in all required fields before proceeding to the next step.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val age = ageText.toIntOrNull()
        if (age == null || age < 18) {
            AlertDialog.Builder(this)
                .setTitle("Invalid Age")
                .setMessage("Please enter a valid age. You must be at least 18 years old.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        // 유효성 검사 통과 후 업로드/저장 실행
        uploadImageAndSaveData(uid)
    }

    /** 이미지 업로드 및 Firestore 저장 로직 분리 **/
    private fun uploadImageAndSaveData(uid: String) {
        // TODO: 로딩 스피너 표시

        if (selectedImageUri != null) {
            // 이미지가 선택된 경우
            val storageRef = storage.reference.child("profile_images/${UUID.randomUUID()}.jpg")
            val uploadTask = storageRef.putFile(selectedImageUri!!)

            uploadTask.addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    // Firestore에 이미지 URL과 함께 저장
                    saveDataToFirestore(uid, uri.toString())
                }
            }.addOnFailureListener { e ->
                Log.e("ProfileSetup", "Image upload failed: ${e.message}")
                // 이미지 업로드가 실패해도, 텍스트 데이터는 저장
                saveDataToFirestore(uid, "")
            }
        } else {
            // 이미지를 선택하지 않은 경우
            saveDataToFirestore(uid, "")
        }
    }

    /** Firestore 저장 실행 함수 **/
    private fun saveDataToFirestore(uid: String, imageUrl: String) {
        // userMap을 사용해 부분 업데이트 (덮어쓰기 방지)
        val userMap = hashMapOf<String, Any>(
            "uid" to uid,
            "userType" to userType, // userType 저장
            "name" to binding.inputName.text.toString().trim(),
            "age" to binding.inputAge.text.toString().trim().toInt(),
            "gender" to binding.spinnerGender.text.toString().trim(),
            "occupation" to binding.spinnerOccupation.text.toString().trim(),
            "mbti" to binding.inputMbti.text.toString().trim(),
            "moveInDate" to binding.inputMoveInDate.text.toString().trim(),
            "profileImageUrl" to imageUrl,
            "timestamp" to System.currentTimeMillis() // 생성/수정 시간
        )

        db.collection("users").document(uid)
            .set(userMap, SetOptions.merge())
            .addOnSuccessListener {
                Log.d("ProfileSetup", "Step 1 data saved for $uid")
                // TODO: 로딩 스피너 숨기기
                goToNextStep() // 저장이 성공해야 다음으로 이동
            }
            .addOnFailureListener { e ->
                Log.e("ProfileSetup", "Firestore save failed: ${e.message}")
                // TODO: 로딩 스피너 숨기기
                Toast.makeText(this, "Error saving data. Please try again.", Toast.LENGTH_SHORT).show()
            }
    }

    /** 다음 단계 Activity로 이동 **/
    private fun goToNextStep() {
        val nextActivity = when (userType) {
            "Provider" -> ProfileSetupB1Activity::class.java // 집 정보 입력
            "Seeker" -> ProfileSetupB2Activity::class.java // 라이프스타일 입력
            "HouseSeeker" -> ProfileSetupB3Activity::class.java // 원하는 집 입력
            else -> null
        }

        nextActivity?.let {
            val intent = Intent(this, it)
            intent.putExtra("USER_TYPE", userType) // userType을 다음 Activity로 전달
            startActivity(intent)
        } ?: Log.e("ProfileSetup", "Unknown userType: $userType")
    }
}