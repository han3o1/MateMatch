package com.mp.matematch.profile.model

// Firestore용 통합 데이터 모델 (모든 프로필 설정 단계 포함)
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

    // B, B2, B3 단계: 거주 관련
    val city: String = "",
    val district: String = "",
    val addressDetail: String = "",
    val budgetMin: Int = 0,
    val budgetMax: Int = 0,
    val roomType: String = "",       // 예: One-room, Shared, Studio 등
    val duration: String = "",       // 예: 6개월, 1년 등

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
    val bio: String = "",              // 자기소개
    val tags: List<String> = emptyList(),  // 관심사나 키워드 (예: ["조용한", "정리정돈"])
    val timestamp: Long = System.currentTimeMillis()
)
