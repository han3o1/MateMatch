package com.mp.matematch.main.ui.feed

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import com.mp.matematch.profile.model.User
import com.mp.matematch.settings.SettingsRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class FeedItem(
    val user: User,
    val matchScore: Int
)

class FeedViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val usersCollection = db.collection("users")

    private val settingsRepo = SettingsRepository
    private val context = application.applicationContext

    // ⭐ 필터 변수 추가
    private var filterLocations: List<String> = emptyList()
    private var filterBuildingTypes: List<String> = emptyList()
    private var filterGender: String? = null
    private var filterSleep: String? = null
    private var filterSmoking: String? = null
    private var filterPets: String? = null
    private var filterCleanliness: String? = null
    private var filterOccupation: String? = null
    private var filterMBTI: String? = null
    private var filterMoveInDate: String? = null

    private val _houseList = MutableLiveData<List<FeedItem>>()
    val houseList: LiveData<List<FeedItem>> = _houseList

    private val _personList = MutableLiveData<List<FeedItem>>()
    val personList: LiveData<List<FeedItem>> = _personList

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private var currentUser: User? = null


    // 🔥 FilterDialog에서 받은 값 저장하는 함수
    fun applyFilters(filters: Map<String, Any?>) {
        filterLocations = filters["locations"] as? List<String> ?: emptyList()
        filterBuildingTypes = filters["buildingTypes"] as? List<String> ?: emptyList()
        filterGender = filters["gender"] as? String
        filterSleep = filters["sleepSchedule"] as? String
        filterSmoking = filters["smoking"] as? String
        filterPets = filters["pets"] as? String
        filterCleanliness = filters["cleanliness"] as? String
        filterOccupation = filters["occupation"] as? String
        filterMBTI = filters["mbti"] as? String
        filterMoveInDate = filters["moveInDate"] as? String

        // 필터 적용 즉시 다시 로드
        loadHouseFeed()
        loadPersonFeed()
    }


    private suspend fun fetchCurrentUser(): User? {
        if (currentUser != null) return currentUser
        val myUid = auth.currentUser?.uid ?: return null

        return try {
            val doc = usersCollection.document(myUid).get().await()
            currentUser = doc.toObject(User::class.java)
            currentUser
        } catch (e: Exception) {
            _error.postValue("내 정보 불러오기 실패.")
            null
        }
    }


    // 🔥 Firestore 쿼리에 필터 적용하는 함수
    private fun applyFirestoreFilters(query: Query): Query {
        var q = query

        if (filterLocations.isNotEmpty()) {
            q = q.whereIn("city", filterLocations)
        }

        if (filterBuildingTypes.isNotEmpty() && !filterBuildingTypes.contains("notLooking")) {
            q = q.whereIn("buildingType", filterBuildingTypes)
        }

        if (!filterGender.isNullOrEmpty() && filterGender != "Any") {
            q = q.whereEqualTo("gender", filterGender)
        }

        if (!filterSleep.isNullOrEmpty() && filterSleep != "Doesn’t Matter") {
            q = q.whereEqualTo("sleepSchedule", filterSleep)
        }

        if (!filterSmoking.isNullOrEmpty() && filterSmoking != "Doesn’t Matter") {
            q = q.whereEqualTo("smoking", filterSmoking)
        }

        if (!filterPets.isNullOrEmpty() && filterPets != "Doesn’t Matter") {
            q = q.whereEqualTo("pets", filterPets)
        }

        if (!filterCleanliness.isNullOrEmpty() && filterCleanliness != "Doesn’t Matter") {
            q = q.whereEqualTo("cleanliness", filterCleanliness)
        }

        if (!filterOccupation.isNullOrEmpty() && filterOccupation != "Any") {
            q = q.whereEqualTo("occupation", filterOccupation)
        }

        if (!filterMBTI.isNullOrEmpty()) {
            q = q.whereEqualTo("mbti", filterMBTI)
        }

        if (!filterMoveInDate.isNullOrEmpty() && filterMoveInDate != "Any") {
            q = q.whereEqualTo("moveInDate", filterMoveInDate)
        }

        return q
    }


    // 🔥 집 찾기 feed
    fun loadHouseFeed() {
        viewModelScope.launch {
            val me = fetchCurrentUser() ?: return@launch

            val target = getTargetUserType(me.userType)
            if (target == "none") {
                _houseList.postValue(emptyList())
                return@launch
            }

            var q = usersCollection.whereEqualTo("userType", target)

            q = applyFirestoreFilters(q)

            fetchFeed(q, _houseList)
        }
    }


    // 🔥 사람 찾기 feed
    fun loadPersonFeed() {
        viewModelScope.launch {
            val me = fetchCurrentUser() ?: return@launch

            val target = getTargetUserType(me.userType)
            if (target == "none") {
                _personList.postValue(emptyList())
                return@launch
            }

            var q = usersCollection.whereEqualTo("userType", target)

            q = applyFirestoreFilters(q)

            fetchFeed(q, _personList)
        }
    }


    private suspend fun fetchFeed(query: Query, liveData: MutableLiveData<List<FeedItem>>) {
        _isLoading.postValue(true)

        val myUser = fetchCurrentUser() ?: return

        try {
            val snaps = query.get().await()
            val others = snaps.toObjects(User::class.java)
            val myUid = auth.currentUser?.uid

            val result = others
                .filter { it.uid != myUid }
                .map { FeedItem(it, calculateMatchPercentage(myUser, it)) }

            liveData.postValue(result)

        } catch (e: Exception) {
            _error.postValue("피드 로딩 실패.")
        } finally {
            _isLoading.postValue(false)
        }
    }


    private fun getTargetUserType(type: String): String {
        return when (type) {
            "Seeker" -> "Seeker"
            "Provider" -> "HouseSeeker"
            "HouseSeeker" -> "Provider"
            else -> "none"
        }
    }


    // 🔥 매칭 알고리즘 (변경 없음)
    private fun calculateMatchPercentage(myUser: User, otherUser: User): Int {
        var score = 0
        val max = 60.0

        if (myUser.prefAgeRange == "Any" ||
            ageMatch(myUser.prefAgeRange, otherUser.age)
        ) score += 10

        if (myUser.prefGender == "Any" || myUser.prefGender == otherUser.gender) score += 10

        if (myUser.prefSleepSchedule == "Doesn’t Matter" ||
            myUser.prefSleepSchedule == otherUser.sleepSchedule
        ) score += 10

        if (myUser.prefSmoking == "Doesn’t Matter" ||
            myUser.prefSmoking == otherUser.smoking
        ) score += 10

        if (myUser.prefPets == "Doesn’t Matter" ||
            myUser.prefPets == otherUser.pets
        ) score += 10

        if (myUser.prefCleanliness == "Doesn’t Matter" ||
            myUser.prefCleanliness == otherUser.cleanliness
        ) score += 10

        return (score / max * 100).toInt()
    }

    private fun ageMatch(range: String, age: Int): Boolean {
        return when (range) {
            "18-20" -> age in 18..20
            "21-25" -> age in 21..25
            "26-30" -> age in 26..30
            "31-35" -> age in 31..35
            "36+" -> age >= 36
            else -> true
        }
    }
}
