package com.mp.matematch.auth.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// UI 상태를 Livedata로 관리
data class AuthUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth

    // 회원가입 UI 상태 Livedata
    private val _signUpState = MutableLiveData<AuthUiState>()
    val signUpState: LiveData<AuthUiState> = _signUpState

    // 로그인 UI 상태 Livedata
    private val _loginState = MutableLiveData<AuthUiState>()
    val loginState: LiveData<AuthUiState> = _loginState

    // F-001: 회원가입 기능
    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _signUpState.value = AuthUiState(isLoading = true) // 1. 로딩 시작
            try {
                auth.createUserWithEmailAndPassword(email, password).await()
                _signUpState.value = AuthUiState(isSuccess = true) // 2. 성공
                // TODO: Firestore에 프로필 기본 정보 저장
            } catch (e: Exception) {
                _signUpState.value = AuthUiState(error = e.message) // 3. 실패
            }
        }
    }

    // F-001: 로그인 기능
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = AuthUiState(isLoading = true) // 1. 로딩 시작
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                _loginState.value = AuthUiState(isSuccess = true) // 2. 성공
            } catch (e: Exception) {
                _loginState.value = AuthUiState(error = e.message) // 3. 실패
            }
        }
    }

    // TODO: 'Continue with Google' 로직 구현
}