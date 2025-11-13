package com.mp.matematch.main.ui.profile

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.mp.matematch.profile.model.User
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val usersCollection = db.collection("users")

    // 현재 사용자 프로필 정보를 담을 LiveData
    private val _userProfile = MutableLiveData<User>()
    val userProfile: LiveData<User> = _userProfile

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    /**
     * 현재 로그인한 사용자의 프로필 정보를 Firestore에서 불러옵니다.
     */
    fun loadUserProfile() {
        val myUid = auth.currentUser?.uid
        if (myUid == null) {
            _error.postValue("로그인 정보를 찾을 수 없습니다.")
            return
        }

        viewModelScope.launch {
            try {
                val document = usersCollection.document(myUid).get().await()
                val user = document.toObject<User>()
                if (user != null) {
                    _userProfile.postValue(user!!)
                    Log.d("ProfileViewModel", "사용자 정보 로드 성공: ${user.name}")
                } else {
                    _error.postValue("사용자 정보를 불러오는데 실패했습니다.")
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "사용자 정보 로드 실패", e)
                _error.postValue(e.message)
            }
        }
    }
}