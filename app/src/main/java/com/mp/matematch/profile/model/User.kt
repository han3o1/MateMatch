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
    val userType: String = "",       // Provider / Seeker / Finder

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
    val sleepSchedule: String = "",  // e.g., "Early riser", "Night owl"
    val smoking: String = "",        // "Yes"/"No"
    val pets: String = "",           // "Yes"/"No"
    val cleanliness: String = "",    // e.g., "Daily", "Weekly"
    val guestPolicy: String = "",    // e.g., "Often", "Rarely"
    val socialPreference: String = "", // e.g., "Introvert", "Extrovert"

    // D단계: Ideal Roommate
    val prefAgeRange: String = "",
    val prefGender: String = "",
    val prefSleepSchedule: String = "",
    val prefSmoking: String = "",
    val prefPets: String = "",
    val prefCleanliness: String = "",

    // E단계: 최종 요약 및 태그
    val statusMessage: String = "",              // 자기소개
    val bio: String = "",
    val timestamp: Long = 0L,

) : Parcelable