package com.mp.matematch

import android.app.Application
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import com.google.firebase.messaging.FirebaseMessaging
import com.kakao.vectormap.KakaoMapSdk

class MateMatchApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // ✅ Firebase 초기화
        Firebase.initialize(this)

        // ✅ Kakao Map SDK 초기화
        KakaoMapSdk.init(this, "306ea9db2d08a4684b5a8e110f9f0a4e")

        // ✅ FCM 토큰 저장
        registerFcmToken()
    }

    private fun registerFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "❌ FCM 토큰 가져오기 실패", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            Log.d("FCM", "✅ 새 FCM Token: $token")

            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                Log.w("FCM", "⚠️ 로그인된 사용자가 없어 토큰을 저장하지 않음")
                return@addOnCompleteListener
            }

            val userDoc = FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)

            userDoc.update("fcmToken", token)
                .addOnSuccessListener {
                    Log.d("FCM", "✅ Firestore에 토큰 저장 완료")
                }
                .addOnFailureListener { e ->
                    Log.e("FCM", "⚠️ Firestore 저장 실패: ${e.message}")
                }
        }
    }
}

