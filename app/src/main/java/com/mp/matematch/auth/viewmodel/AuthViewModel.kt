package com.mp.matematch.auth.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.GoogleAuthProvider

// UI 상태를 Livedata로 관리
data class AuthUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val isNewUser: Boolean? = null,
    val error: String? = null
)

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth
    private val db = Firebase.firestore

    // 회원가입 UI 상태 Livedata
    private val _signUpState = MutableLiveData<AuthUiState>()
    val signUpState: LiveData<AuthUiState> = _signUpState

    // 로그인 UI 상태 Livedata
    private val _loginState = MutableLiveData<AuthUiState>()
    val loginState: LiveData<AuthUiState> = _loginState

    // F-001: 회원가입 기능
    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _signUpState.value = AuthUiState(isLoading = true)
            try {
                auth.createUserWithEmailAndPassword(email, password).await()
                _signUpState.value = AuthUiState(isSuccess = true, isNewUser = true)
            } catch (e: Exception) {
                _signUpState.value = AuthUiState(error = e.message)
            }
        }
    }

    // F-001: 로그인 기능
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = AuthUiState(isLoading = true)
            try {
                val authResult = auth.signInWithEmailAndPassword(email, password).await()
                val uid = authResult.user?.uid ?: throw Exception("Failed to get user ID.")

                checkProfileAndSetLoginState(uid)

            } catch (e: Exception) {
                _loginState.value = AuthUiState(error = e.message)
            }
        }
    }

    // 구글 소셜 로그인 기능
    fun firebaseAuthWithGoogle(idToken: String) {
        viewModelScope.launch {
            _loginState.value = AuthUiState(isLoading = true)
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(credential).await()
                val uid = authResult.user?.uid ?: throw Exception("Failed to get user ID from Google.")

                checkProfileAndSetLoginState(uid)

            } catch (e: Exception) {
                _loginState.value = AuthUiState(error = e.message)
            }
        }
    }

    // 프로필 존재 여부 확인 및 상태 업데이트
    private suspend fun checkProfileAndSetLoginState(uid: String) {
        try {
            val document = db.collection("users").document(uid).get().await()
            if (document.exists()) {
                Log.d("AuthViewModel", "Existing user logged in.")
                // 프로필이 존재 -> 기존 유저
                _loginState.value = AuthUiState(isSuccess = true, isNewUser = false)
            } else {
                Log.d("AuthViewModel", "New user logged in.")
                // 프로필이 없음 -> 신규 유저
                _loginState.value = AuthUiState(isSuccess = true, isNewUser = true)
            }
        } catch (e: Exception) {
            throw Exception("Failed to check user profile: ${e.message}")
        }
    }
}