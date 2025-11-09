package com.mp.matematch.profile.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mp.matematch.profile.model.User

class ProfileViewModel : ViewModel() {

    // Firestore 및 Auth 참조
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // LiveData로 UI 반영
    private val _user = MutableLiveData<User>()
    val user: LiveData<User> get() = _user

    private val _isSaving = MutableLiveData<Boolean>()
    val isSaving: LiveData<Boolean> get() = _isSaving

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    init {
        loadUserProfile() // 로그인된 사용자가 있으면 불러오기
    }

    /**
     * Firestore에서 현재 로그인된 사용자의 프로필 로드
     */
    fun loadUserProfile() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val loadedUser = document.toObject(User::class.java)
                    _user.value = loadedUser ?: User(uid = uid)
                } else {
                    _user.value = User(uid = uid)
                }
            }
            .addOnFailureListener { e ->
                _errorMessage.value = "프로필 로드 실패: ${e.message}"
            }
    }

    /**
     * 프로필 데이터 변경 시 UI → ViewModel 반영
     */
    fun updateField(fieldName: String, value: Any) {
        val current = _user.value ?: User(uid = auth.currentUser?.uid ?: "")
        val updated = when (fieldName) {
            "name" -> current.copy(name = value.toString())
            "age" -> current.copy(age = (value as? Int) ?: 0)
            "occupation" -> current.copy(occupation = value.toString())
            "gender" -> current.copy(gender = value.toString())
            "mbti" -> current.copy(mbti = value.toString())
            "moveInDate" -> current.copy(moveInDate = value.toString())
            "profileImageUrl" -> current.copy(profileImageUrl = value.toString())
            "city" -> current.copy(city = value.toString())
            "district" -> current.copy(district = value.toString())
            "addressDetail" -> current.copy(addressDetail = value.toString())
            "budgetMin" -> current.copy(budgetMin = (value as? Int) ?: 0)
            "budgetMax" -> current.copy(budgetMax = (value as? Int) ?: 0)
            "roomType" -> current.copy(roomType = value.toString())
            "duration" -> current.copy(duration = value.toString())
            "sleepSchedule" -> current.copy(sleepSchedule = value.toString())
            "smoking" -> current.copy(smoking = value.toString())
            "pets" -> current.copy(pets = value.toString())
            "cleanliness" -> current.copy(cleanliness = value.toString())
            "guestPolicy" -> current.copy(guestPolicy = value.toString())
            "socialPreference" -> current.copy(socialPreference = value.toString())
            "prefAgeRange" -> current.copy(prefAgeRange = value.toString())
            "prefGender" -> current.copy(prefGender = value.toString())
            "prefSleepSchedule" -> current.copy(prefSleepSchedule = value.toString())
            "prefSmoking" -> current.copy(prefSmoking = value.toString())
            "prefPets" -> current.copy(prefPets = value.toString())
            "prefCleanliness" -> current.copy(prefCleanliness = value.toString())
            "bio" -> current.copy(bio = value.toString())
            "tags" -> current.copy(tags = value as? List<String> ?: emptyList())
            else -> current
        }
        _user.value = updated
    }


    /**
     * Firestore에 프로필 저장
     */
    fun saveUserProfile(onComplete: (Boolean) -> Unit) {
        val userToSave = _user.value ?: return
        _isSaving.value = true

        db.collection("users")
            .document(userToSave.uid)
            .set(userToSave)
            .addOnSuccessListener {
                _isSaving.value = false
                onComplete(true)
            }
            .addOnFailureListener { e ->
                _isSaving.value = false
                _errorMessage.value = "프로필 저장 실패: ${e.message}"
                onComplete(false)
            }
    }

    /**
     * 상태 초기화 (에러 메시지 등)
     */
    fun clearError() {
        _errorMessage.value = null
    }
}
