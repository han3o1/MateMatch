package com.mp.matematch.profile.model
import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

// Firestore용 통합 데이터 모델 (모든 프로필 설정 단계 포함)
@Keep
@Parcelize
data class User(
    // 공통 정보 (Firebase)
    val uid: String = "",
    val userType: String = "",       // Provider / Seeker / HouseSeeker

    // A단계: 기본 프로필
    val name: String = "",
    val age: Int = 0,
    val gender: String = "",
    val occupation: String = "",
    val mbti: String = "",
    val moveInDate: String = "",
    val profileImageUrl: String = "",

    // B1, B2, B3 단계: 거주 관련
    val city: String = "",
    val district: String = "",
    val buildingType: String? = null,
    val monthlyRent: Int? = null,
    val maintenanceFee: Int? = null,
    val amenities: List<String>? = null,

    // C단계: Lifestyle
    val sleepSchedule: String = "",
    val smoking: String = "",
    val pets: String = "",
    val cleanliness: String = "",
    val guestPolicy: String = "",
    val socialPreference: String = "",

    // D단계: Ideal Roommate
    val prefAgeRange: String = "",
    val prefGender: String = "",
    val prefSleepSchedule: String = "",
    val prefSmoking: String = "",
    val prefPets: String = "",
    val prefCleanliness: String = "",

    // E단계: 최종 요약 및 태그
    val statusMessage: String = "",
    val bio: String = "",
    val timestamp: Long = 0L,

    // 매칭 로직
    val likedUsers: Map<String, Boolean> = emptyMap()

) : Parcelable