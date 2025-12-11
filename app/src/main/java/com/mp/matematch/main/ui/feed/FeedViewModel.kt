package com.mp.matematch.main.ui.feed

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import com.mp.matematch.profile.model.User
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class FeedItem(
    val user: User,
    val matchScore: Int
)

data class FilterOptions(
    val cities: List<String>,
    val buildingTypes: List<String>,
    val sleepSchedules: List<String>,
    val cleanliness: List<String>,
    val occupations: List<String>,
    val smoking: List<String>,
    val pets: List<String>,
    val moveInDates: List<String>
)

class FeedViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val usersCollection = db.collection("users")

    private var currentUser: User? = null

    // -----------------------------
    // LiveData
    // -----------------------------
    private val _houseList = MutableLiveData<List<FeedItem>>()
    val houseList: LiveData<List<FeedItem>> = _houseList

    private val _personList = MutableLiveData<List<FeedItem>>()
    val personList: LiveData<List<FeedItem>> = _personList

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error


    // -----------------------------
    // 필터 변수
    // -----------------------------
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


    // ===========================================================
    // 🔥 Firestore에서 모든 사용자 불러와 distinct 옵션 만들기
    // ===========================================================
    suspend fun loadFilterOptions(): FilterOptions {
        val snaps = usersCollection.get().await()
        val allUsers = snaps.toObjects(User::class.java)

        return FilterOptions(
            cities = allUsers.mapNotNull { it.city }.distinct(),
            buildingTypes = allUsers.mapNotNull { it.buildingType }.distinct(),
            sleepSchedules = allUsers.mapNotNull { it.sleepSchedule }.distinct(),
            cleanliness = allUsers.mapNotNull { it.cleanliness }.distinct(),
            occupations = allUsers.mapNotNull { it.occupation }.distinct(),
            smoking = allUsers.mapNotNull { it.smoking }.distinct(),
            pets = allUsers.mapNotNull { it.pets }.distinct(),
            moveInDates = allUsers.mapNotNull { it.moveInDate }.distinct()
        )
    }


    // ===========================================================
    // 🔥 필터 적용
    // ===========================================================
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

        loadHouseFeed()
        loadPersonFeed()
    }


    // ===========================================================
    // 🔥 내 정보 가져오기
    // ===========================================================
    private suspend fun fetchCurrentUser(): User? {
        if (currentUser != null) return currentUser

        val uid = auth.currentUser?.uid ?: return null

        return try {
            val doc = usersCollection.document(uid).get().await()
            currentUser = doc.toObject(User::class.java)
            currentUser
        } catch (e: Exception) {
            _error.postValue("내 정보 불러오기 실패")
            null
        }
    }


    // ===========================================================
    // 🔥 날짜 필터 (Within 1/3 months)
    // ===========================================================
    private fun isMoveInDateInRange(moveInDate: String?, option: String): Boolean {
        if (moveInDate == null) return false
        if (option == "Any") return true

        return try {
            val formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")
            val date = LocalDate.parse(moveInDate, formatter)
            val today = LocalDate.now()

            when (option) {
                "Within 1 month" -> date in today..today.plusMonths(1)
                "Within 3 months" -> date in today..today.plusMonths(3)
                else -> true
            }
        } catch (_: Exception) {
            true
        }
    }


    // ===========================================================
    // 🔥 Feed 로딩 함수 (집)
    // ===========================================================
    fun loadHouseFeed() {
        viewModelScope.launch {
            val me = fetchCurrentUser() ?: return@launch
            val targetType = getTargetUserType(me.userType)

            var q = usersCollection.whereEqualTo("userType", targetType)
            fetchFeed(q, _houseList)
        }
    }

    // ===========================================================
    // 🔥 Feed 로딩 함수 (사람)
    // ===========================================================
    fun loadPersonFeed() {
        viewModelScope.launch {
            val me = fetchCurrentUser() ?: return@launch
            val targetType = getTargetUserType(me.userType)

            var q = usersCollection.whereEqualTo("userType", targetType)
            fetchFeed(q, _personList)
        }
    }


    // ===========================================================
    // 🔥 실제 Firestore 읽고 필터 적용
    // ===========================================================
    private suspend fun fetchFeed(
        query: Query,
        liveData: MutableLiveData<List<FeedItem>>
    ) {
        _isLoading.postValue(true)

        val myUser = fetchCurrentUser() ?: return

        try {
            val snaps = query.get().await()
            var users = snaps.toObjects(User::class.java)

            val myUid = auth.currentUser?.uid
            users = users.filter { it.uid != myUid }

            // --------------------------
            // 🔥 Step1 + Step2 필터링
            // --------------------------
            if (filterLocations.isNotEmpty()) {
                users = users.filter { filterLocations.contains(it.city) }
            }

            if (filterBuildingTypes.isNotEmpty()) {
                users = users.filter { filterBuildingTypes.contains(it.buildingType) }
            }

            if (!filterGender.isNullOrEmpty() && filterGender != "Any") {
                users = users.filter { it.gender == filterGender }
            }

            if (!filterSleep.isNullOrEmpty() && filterSleep != "Any") {
                users = users.filter { it.sleepSchedule == filterSleep }
            }

            if (!filterSmoking.isNullOrEmpty() && filterSmoking != "Any") {
                users = users.filter { it.smoking == filterSmoking }
            }

            if (!filterPets.isNullOrEmpty() && filterPets != "Any") {
                users = users.filter { it.pets == filterPets }
            }

            if (!filterCleanliness.isNullOrEmpty() && filterCleanliness != "Any") {
                users = users.filter { it.cleanliness == filterCleanliness }
            }

            if (!filterOccupation.isNullOrEmpty() && filterOccupation != "Any") {
                users = users.filter { it.occupation == filterOccupation }
            }

            if (!filterMBTI.isNullOrEmpty()) {
                users = users.filter { it.mbti == filterMBTI }
            }

            if (!filterMoveInDate.isNullOrEmpty() && filterMoveInDate != "Any") {
                users = users.filter {
                    isMoveInDateInRange(it.moveInDate, filterMoveInDate!!)
                }
            }

            // --------------------------
            // 🔥 매칭 점수 계산
            // --------------------------
            val result = users.map {
                FeedItem(it, calculateMatchPercentage(myUser, it))
            }

            liveData.postValue(result)

        } catch (e: Exception) {
            _error.postValue("피드 로드 실패: ${e.message}")
        } finally {
            _isLoading.postValue(false)
        }
    }


    // ===========================================================
    // 🔥 유저타입 매칭
    // ===========================================================
    private fun getTargetUserType(type: String): String {
        return when (type) {
            "Seeker" -> "Provider"
            "Provider" -> "HouseSeeker"
            "HouseSeeker" -> "Provider"
            else -> "none"
        }
    }


    // ===========================================================
    // 🔥 매칭 점수 계산 (기존대로)
    // ===========================================================
    private fun calculateMatchPercentage(myUser: User, otherUser: User): Int {
        var score = 0
        val max = 60.0

        if (myUser.prefAgeRange == "Any" ||
            ageMatch(myUser.prefAgeRange, otherUser.age)
        ) score += 10

        if (myUser.prefGender == "Any" || myUser.prefGender == otherUser.gender) score += 10

        if (myUser.prefSleepSchedule == "Any" ||
            myUser.prefSleepSchedule == otherUser.sleepSchedule
        ) score += 10

        if (myUser.prefSmoking == "Any" ||
            myUser.prefSmoking == otherUser.smoking
        ) score += 10

        if (myUser.prefPets == "Any" ||
            myUser.prefPets == otherUser.pets
        ) score += 10

        if (myUser.prefCleanliness == "Any" ||
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
