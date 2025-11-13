package com.mp.matematch.profile.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.mp.matematch.profile.model.User
import kotlin.String

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
            // A단계: 기본 프로필
            "name" -> current.copy(name = value.toString())
            "age" -> current.copy(age = (value as? Int) ?: 0)
            "gender" -> current.copy(gender = value.toString())
            "occupation" -> current.copy(occupation = value.toString())
            "mbti" -> current.copy(mbti = value.toString())
            "moveInDate" -> current.copy(moveInDate = value.toString())
            "profileImageUrl" -> current.copy(profileImageUrl = value.toString())

            // B, B2, B3 단계: 거주 관련
            "city" -> current.copy(city = value.toString())
            "district" -> current.copy(district = value.toString())
            "buildingType" -> current.copy(buildingType = value.toString())
            "monthlyRent" -> current.copy(monthlyRent = (value as? Int) ?: 0)
            "maintenanceFee" -> current.copy(maintenanceFee = (value as? Int) ?: 0)
            "amenities" -> current.copy(amenities = value as? List<String> ?: emptyList())

            // C단계: Lifestyle
            "sleepSchedule" -> current.copy(sleepSchedule = value.toString())
            "smoking" -> current.copy(smoking = value.toString())
            "pets" -> current.copy(pets = value.toString())
            "cleanliness" -> current.copy(cleanliness = value.toString())
            "guestPolicy" -> current.copy(guestPolicy = value.toString())
            "socialPreference" -> current.copy(socialPreference = value.toString())

            // D단계: Ideal Roommate
            "prefAgeRange" -> current.copy(prefAgeRange = value.toString())
            "prefGender" -> current.copy(prefGender = value.toString())
            "prefSleepSchedule" -> current.copy(prefSleepSchedule = value.toString())
            "prefSmoking" -> current.copy(prefSmoking = value.toString())
            "prefPets" -> current.copy(prefPets = value.toString())
            "prefCleanliness" -> current.copy(prefCleanliness = value.toString())

            // E단계: 최종 요약 및 태그
            "statusMessage" -> current.copy(statusMessage = value.toString())
            "bio" -> current.copy(bio = value.toString())

            "userType" -> current.copy(userType = value.toString())

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
            .set(userToSave, SetOptions.merge())
            .addOnSuccessListener {
                _isSaving.value = false
                Log.d("ProfileViewModel", "Profile successfully merged!")
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
