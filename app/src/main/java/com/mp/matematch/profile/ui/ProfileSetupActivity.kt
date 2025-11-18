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
import android.graphics.Bitmap
import android.provider.MediaStore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions

class ProfileSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileSetupABinding
    private lateinit var userType: String
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private var selectedImageUri: Uri? = null

    // 이미지 선택 결과 처리를 위한 Launcher 등록
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            if (selectedImageUri != null) {
                try {
                    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    contentResolver.takePersistableUriPermission(selectedImageUri!!, takeFlags)
                    Log.d("ProfileSetup", "Persistent URI permission granted.")
                } catch (e: SecurityException) {
                    Log.e("ProfileSetup", "Failed to take persistable URI permission", e)
                }
            }
            binding.profileImage.setImageURI(selectedImageUri)
        }
    }

    // ML Kit 한글 텍스트 인식기 인스턴스
    private val recognizer = TextRecognition.getClient(
        KoreanTextRecognizerOptions.Builder().build()
    )

    // 카메라 앱 실행 결과 처리를 위한 Launcher
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // 카메라 앱에서 이미지를 비트맵으로 받아옴
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            if (imageBitmap != null) {
                // 받아온 비트맵으로 텍스트 인식 실행
                runTextRecognition(imageBitmap)
            } else {
                Toast.makeText(this, "Failed to capture image.", Toast.LENGTH_SHORT).show()
            }
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

        // '학생증/사원증 스캔' 버튼 클릭 리스너
        binding.btnScanId.setOnClickListener {
            dispatchTakePictureIntent()
        }

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
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                flags = flags or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            pickImageLauncher.launch(intent)
        }

        // '다음' 버튼 클릭 시 유효성 검사 및 저장
        binding.btnNext.setOnClickListener {
            validateAndSave()
        }

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
        if (selectedImageUri != null) {
            // 이미지가 선택된 경우
            val storageRef = storage.reference.child("profile_images/${UUID.randomUUID()}.jpg")
            try {
                val inputStream = contentResolver.openInputStream(selectedImageUri!!)
                val uploadTask = storageRef.putStream(inputStream!!)

                uploadTask.addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        // Firestore에 이미지 URL과 함께 저장
                        saveDataToFirestore(uid, uri.toString())
                    }
                }.addOnFailureListener { e ->
                    // 업로드 실패 시 (네트워크 오류, Storage 규칙 등)
                    Log.e("ProfileSetup", "Image upload failed: ${e.message}", e)
                    saveDataToFirestore(uid, "")
                }
            } catch (e: Exception) {
                // openInputStream 자체에서 실패할 경우 (권한 오류 등)
                Log.e("ProfileSetup", "Failed to open InputStream: ${e.message}", e)
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
                goToNextStep() // 저장이 성공해야 다음으로 이동
            }
            .addOnFailureListener { e ->
                Log.e("ProfileSetup", "Firestore save failed: ${e.message}")
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

    /** (OCR 1) 카메라 인텐트 실행 함수 */
    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            if (takePictureIntent.resolveActivity(packageManager) != null) {
                takePictureLauncher.launch(takePictureIntent)
            } else {
                Log.e("OCR", "No camera app found.")
                Toast.makeText(this, "Camera app not found.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** (OCR 2) ML Kit 텍스트 인식 실행 함수 */
    private fun runTextRecognition(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                // 텍스트 인식 성공
                Log.d("OCR", "Full Text: ${visionText.text}")
                // 텍스트를 파싱하여 UI에 적용
                parseTextAndAutofill(visionText.text)
            }
            .addOnFailureListener { e ->
                // 텍스트 인식 실패
                Log.e("OCR", "Text recognition failed", e)
                Toast.makeText(this, "Text recognition failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /** (OCR 3) 텍스트 파싱 및 자동 채우기 함수 */
    private fun parseTextAndAutofill(fullText: String) {
        val lines = fullText.split("\n")

        var foundName: String? = null
        var foundSchool: String? = null

        for (line in lines) {
            Log.d("OCR_Parse", "Line: $line")

            // (1) 학교/회사 찾기 (00님 학교 '서울과학기술대학교' 키워드 추가)
            if (line.contains("대학교") || line.contains("University") || line.contains("서울과학기술대학교")) {
                foundSchool = line.trim()
                Log.d("OCR_Parse", "Found School: $foundSchool")
            }

            // (2) 이름 찾기 (예: '성명', '이름' 키워드)
            if (line.startsWith("성명") || line.startsWith("이름")) {
                foundName = line.split(":", " ").lastOrNull()?.trim()
                Log.d("OCR_Parse", "Found Name: $foundName")
            }
        }

        // 찾은 텍스트를 EditText에 자동으로 채워넣기
        foundName?.let {
            binding.inputName.setText(it)
        }
        foundSchool?.let {
            binding.spinnerOccupation.setText(it, false)
        }

        if (foundName == null && foundSchool == null) {
            Toast.makeText(this, "Could not find valid name or occupation.", Toast.LENGTH_SHORT).show()
        }
    }
}