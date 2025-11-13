package com.mp.matematch.main.ui.feed

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import com.mp.matematch.profile.model.User

class FeedViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val usersCollection = db.collection("users")

    // '집 목록' 피드를 위한 LiveData (그룹 A)
    private val _houseList = MutableLiveData<List<User>>()
    val houseList: LiveData<List<User>> = _houseList

    // '사람 목록' 피드를 위한 LiveData (그룹 C)
    private val _personList = MutableLiveData<List<User>>()
    val personList: LiveData<List<User>> = _personList

    /**
     * '집 찾기' 피드(그룹 A: Provider)를 불러오는 함수
     */
    fun loadHouseFeed() {
        usersCollection
            .whereEqualTo("userType", "Provider")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val users = querySnapshot.toObjects<User>()
                _houseList.value = users
                Log.d("FeedViewModel", "집 피드 로드 완료: ${users.size}개")
            }
            .addOnFailureListener {
                Log.e("FeedViewModel", "집 피드 로드 실패", it)
            }
    }

    /**
     * '집 찾기' 피드에 필터 적용
     */
    fun applyHouseFilters(filters: Map<String, Any?>) {
        var query: Query = usersCollection
            .whereEqualTo("userType", "Provider")

        val locations = filters["locations"] as? List<*> ?: emptyList<String>()
        if (locations.isNotEmpty()) query = query.whereIn("city", locations)

        val buildingTypes = filters["buildingTypes"] as? List<*> ?: emptyList<String>()
        if (buildingTypes.isNotEmpty())
            query = query.whereIn("buildingType", buildingTypes)

        // TODO: 월세(monthlyRent), 편의시설(amenities) 등 필터 로직 추가

        query.get()
            .addOnSuccessListener { result ->
                val filtered = result.documents.mapNotNull { it.toObject(User::class.java) }
                _houseList.value = filtered
                Log.d("FeedViewModel", "필터 적용 결과: ${filtered.size}개")
            }.addOnFailureListener {
                Log.e("FeedViewModel", "필터 적용 실패", it)
            }
    }

    /**
     * '사람 찾기' 피드를 불러오는 함수
     */
    fun loadPersonFeed(userType: String?) {
        val targetUserType = when (userType) {
            "Provider" -> "HouseSeeker"
            "Seeker" -> "Seeker"
            else -> "None"       // 그 외 (오류 방지)
        }

        usersCollection
            .whereEqualTo("userType", targetUserType)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val users = querySnapshot.toObjects<User>()
                _personList.value = users
            }
            .addOnFailureListener {
                Log.e("FeedViewModel", "사람 피드 로드 실패", it)
            }
    }

    /**
     * '사람 찾기' 피드에 필터 적용
     */
    fun applyPersonFilters(filters: Map<String, Any?>, userType: String?) {
        val targetUserType = when (userType) {
            "Provider" -> "HouseSeeker"
            "Seeker" -> "Seeker"
            else -> "None"
        }

        var query: Query = usersCollection
            .whereEqualTo("userType", targetUserType)

        // TODO: '사람 찾기'에 필요한 필터 로직 추가
        val locations = filters["locations"] as? List<*> ?: emptyList<String>()
        if (locations.isNotEmpty()) query = query.whereIn("city", locations)

        query.get()
            .addOnSuccessListener { result ->
                val filtered = result.documents.mapNotNull { it.toObject(User::class.java) }
                _personList.value = filtered
            }.addOnFailureListener {
                Log.e("FeedViewModel", "사람 피드 필터 적용 실패", it)
            }
    }
}