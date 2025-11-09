package com.mp.matematch.main.ui.feed

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import com.mp.matematch.profile.model.User // User 모델 임포트

class FeedViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val usersCollection = db.collection("users") // users 컬렉션을 변수로 저장

    // 1. '사람 목록' 피드를 위한 LiveData (그룹 C)
    private val _personList = MutableLiveData<List<User>>()
    val personList: LiveData<List<User>> = _personList

    // 2. '집 목록' 피드를 위한 LiveData (그룹 A)
    private val _houseList = MutableLiveData<List<User>>()
    val houseList: LiveData<List<User>> = _houseList

    /**
     * '사람 찾기' 피드(그룹 C)를 불러오는 함수
     * (FeedPersonalFragment가 호출)
     */
    fun loadPersonFeed() {
        // userType이 "RoommateSeeker"인 사용자만 필터링
        usersCollection.whereEqualTo("userType", "RoommateSeeker")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val users = querySnapshot.toObjects<User>()
                _personList.value = users
            }
            .addOnFailureListener { exception ->
                // TODO: 오류 처리
            }
    }

    /**
     * '집 찾기' 피드(그룹 A)를 불러오는 함수
     * (FeedHouseFragment가 호출)
     */
    fun loadHouseFeed() {
        // userType이 "HomeProvider"인 사용자만 필터링
        usersCollection.whereEqualTo("userType", "HomeProvider")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val users = querySnapshot.toObjects<User>()
                _houseList.value = users
            }
            .addOnFailureListener { exception ->
                // TODO: 오류 처리
            }
    }
}