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
                val uid = authResult.user?.uid

                if (uid == null) {
                    _loginState.value = AuthUiState(error = "Failed to get user ID.")
                    return@launch
                }

                db.collection("users").document(uid).get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            Log.d("AuthViewModel", "Existing user logged in.")
                            _loginState.value = AuthUiState(isSuccess = true, isNewUser = false)
                        } else {
                            Log.d("AuthViewModel", "New user logged in.")
                            _loginState.value = AuthUiState(isSuccess = true, isNewUser = true)
                        }
                    }
                    .addOnFailureListener {
                        _loginState.value = AuthUiState(error = "Failed to check profile.")
                    }

            } catch (e: Exception) {
                _loginState.value = AuthUiState(error = e.message)
            }
        }
    }
}