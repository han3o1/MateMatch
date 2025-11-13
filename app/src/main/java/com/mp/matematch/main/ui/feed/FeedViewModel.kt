package com.mp.matematch.main.ui.feed

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import com.mp.matematch.profile.model.User
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class FeedItem(
    val user: User,
    val matchScore: Int
)

class FeedViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val usersCollection = db.collection("users")

    // '집 목록' 피드를 위한 LiveData (그룹 A)
    private val _houseList = MutableLiveData<List<FeedItem>>()
    val houseList: LiveData<List<FeedItem>> = _houseList

    // '사람 목록' 피드를 위한 LiveData (그룹 C)
    private val _personList = MutableLiveData<List<FeedItem>>()
    val personList: LiveData<List<FeedItem>> = _personList

    // 로딩 상태와 오류 메시지를 UI에 알리기 위한 LiveData
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    // 현재 사용자 프로필을 캐시할 변수
    private var currentUser: User? = null

    /**
     * 현재 사용자 정보를 가져오는 공통 함수
     */
    private suspend fun fetchCurrentUser(): User? {
        // 이미 캐시되어 있으면 바로 반환
        if (currentUser != null) return currentUser

        val myUid = auth.currentUser?.uid ?: return null
        return try {
            val myDoc = usersCollection.document(myUid).get().await()
            currentUser = myDoc.toObject(User::class.java) // User 객체로 변환
            currentUser // 반환
        } catch (e: Exception) {
            Log.e("FeedViewModel", "현재 사용자 정보 로드 실패", e)
            _error.postValue("내 정보를 불러오는데 실패했습니다.")
            null
        }
    }

    /**
     * 쿼리를 실행하고 FeedItem 리스트로 변환하는 공통 로직
     */
    private suspend fun processQueryToFeedItems(query: Query, liveData: MutableLiveData<List<FeedItem>>) {
        _isLoading.postValue(true)

        // 1. '내' 정보 가져오기 (필수)
        val myUser = fetchCurrentUser()
        if (myUser == null) {
            _isLoading.postValue(false)
            _error.postValue("내 정보가 없어 매칭 점수를 계산할 수 없습니다.")
            liveData.postValue(emptyList()) // 내 정보 없으면 빈 리스트 처리
            return
        }

        try {
            // 2. '다른 사용자들' 쿼리 실행
            val snapshot = query.get().await()
            val otherUsers = snapshot.toObjects(User::class.java)
            val myUid = auth.currentUser?.uid

            Log.d("FeedViewModel", "쿼리 userType=${query}, 결과 개수 = ${snapshot.size()}")

            // 3. FeedItem 리스트로 변환 (점수 계산 포함)
            val feedItems = otherUsers
                .filter { it.uid != myUid } // 확실하게 '나'를 제외
                .map { otherUser ->
                    // 4. 궁합 점수 계산
                    val score = calculateMatchPercentage(myUser, otherUser)
                    FeedItem(user = otherUser, matchScore = score)
                }

            liveData.postValue(feedItems)
            Log.d("FeedViewModel", "피드 처리 완료: ${feedItems.size}개")

        } catch (e: Exception) {
            Log.e("FeedViewModel", "피드 쿼리 실패", e)
            _error.postValue("피드를 불러오는 중 오류가 발생했습니다.")
            liveData.postValue(emptyList())
        } finally {
            _isLoading.postValue(false)
        }
    }

    /**
     * 현재 로그인한 유저 타입 → 어떤 userType을 불러올지 매핑
     */
    private fun getTargetUserType(currentType: String): String {
        return when (currentType) {

            // 1) 집 없고 룸메 찾는 사람
            "Seeker" -> "Seeker"

            // 2) 집 있고 룸메 찾는 사람
            "Provider" -> "HouseSeeker"

            // 3) 집 찾는 사람
            "HouseSeeker" -> "Provider"

            else -> "none"
        }
    }


    /**
     * '집 찾기' 피드(그룹 A: Provider)를 불러오는 함수
     */
    fun loadHouseFeed() {
        viewModelScope.launch {
            val me = fetchCurrentUser() ?: return@launch

            // 로그인한 유저 기반으로 타겟 결정
            val target = getTargetUserType(me.userType)

            if (target == "none") {
                _houseList.postValue(emptyList())
                return@launch
            }

            val query = usersCollection.whereEqualTo("userType", target)

            processQueryToFeedItems(query, _houseList)
        }
    }


    /**
     * '집 찾기' 피드에 필터 적용
     */
    fun applyHouseFilters(filters: Map<String, Any?>) {
        viewModelScope.launch {
            var query: Query = usersCollection
                .whereEqualTo("userType", "Provider")

            val locations = filters["locations"] as? List<*> ?: emptyList<FeedItem>()
            if (locations.isNotEmpty()) query = query.whereIn("city", locations)

            val buildingTypes = filters["buildingTypes"] as? List<*> ?: emptyList<FeedItem>()
            if (buildingTypes.isNotEmpty())
                query = query.whereIn("buildingType", buildingTypes)

            // TODO: 필터 로직 ...

            processQueryToFeedItems(query, _houseList)
        }
    }

    /**
     * '사람 찾기' 피드를 불러오는 함수
     */
    fun loadPersonFeed() {
        viewModelScope.launch {

            // 현재 로그인 유저 정보 가져오기
            val me = fetchCurrentUser() ?: return@launch

            // 현재 유저 타입 → 타겟 매핑
            val target = getTargetUserType(me.userType)

            if (target == "none") {
                _personList.postValue(emptyList())
                return@launch
            }

            val query = usersCollection.whereEqualTo("userType", target)

            processQueryToFeedItems(query, _personList)
        }
    }


    /**
     * '사람 찾기' 피드에 필터 적용
     */
    fun applyPersonFilters(filters: Map<String, Any?>) {
        viewModelScope.launch {
            val me = fetchCurrentUser() ?: return@launch
            val target = getTargetUserType(me.userType)

            if (target == "none") {
                _personList.postValue(emptyList())
                return@launch
            }

            var query: Query = usersCollection.whereEqualTo("userType", target)

            val locations = filters["locations"] as? List<*> ?: emptyList<String>()
            if (locations.isNotEmpty())
                query = query.whereIn("city", locations)

            // TODO: 다른 필터 추가

            processQueryToFeedItems(query, _personList)
        }
    }



    /**
     * F-007: 매칭 알고리즘 (궁합 점수 계산)
     */
    private fun calculateMatchPercentage(myUser: User, otherUser: User): Int {
        var score = 0
        val maxPossibleScore = 60.0 // 총 60점 만점, 6개 항목 비교 (각 10점)

        // 1. 나이대 선호도 비교
        val myAgePref = myUser.prefAgeRange // 내가 선호하는 나이대
        val otherAge = otherUser.age // 상대방의 실제 나이대
        try {
            when (myAgePref) {
                "Any" -> score += 10
                "18-20" -> {
                    if (otherAge in 18..20) score += 10
                }
                "21-25" -> {
                    if (otherAge in 21..25) score += 10
                }
                "26-30" -> {
                    if (otherAge in 26..30) score += 10
                }
                "31-35" -> {
                    if (otherAge in 31..35) score += 10
                }
                "36+" -> {
                    if (otherAge >= 36) score += 10
                }
            }
        } catch (e: Exception) {
            Log.e("MatchAlgorithm", "Age parsing failed: ${e.message}")
        }

        // 2. 성별 선호도 비교
        val myGenderPref = myUser.prefGender // 내가 선호하는 성별
        val otherGender = otherUser.gender // 상대방의 실제 성별
        if (myGenderPref == "Any" || myGenderPref == otherGender) {
            score += 10
        }

        // 3. 수면 스케줄 비교
        val mySleepPref = myUser.prefSleepSchedule // 내가 선호하는 수면
        val otherSleep = otherUser.sleepSchedule // 상대방의 수면
        if (mySleepPref == "Doesn’t Matter" || mySleepPref == otherSleep) {
            score += 10
        }

        // 4. 흡연 여부 비교
        val mySmokingPref = myUser.prefSmoking // 내가 선호하는 흡연
        val otherSmoking = otherUser.smoking // 상대방의 흡연
        if (mySmokingPref == "Doesn’t Matter" || mySmokingPref == otherSmoking) {
            score += 10
        }

        // 5. 반려동물 비교
        val myPetPref = myUser.prefPets // 내가 선호하는 반려동물
        val otherPet = otherUser.pets // 상대방의 반려동물
        if (myPetPref == "Doesn’t Matter" || myPetPref == otherPet) {
            score += 10
        }

        // 6. 청결도 비교
        val myCleanPref = myUser.prefCleanliness // 내가 선호하는 청결도
        val otherClean = otherUser.cleanliness // 상대방의 청결도
        if (myCleanPref == "Doesn’t Matter" || myCleanPref == otherClean) {
            score += 10
        }

        if (maxPossibleScore == 0.0) return 0 // 0으로 나누기 방지
        return (score / maxPossibleScore * 100).toInt() // 퍼센트 형식으로 반환
    }
}