package com.mp.matematch

import android.app.Application
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import com.kakao.vectormap.KakaoMapSdk


class MateMatchApplication : Application() { // 👈 이 부분이 중요합니다.
    override fun onCreate() {
        super.onCreate()
        // 앱이 시작될 때 Firebase를 초기화합니다.
        Firebase.initialize(this)
        // ✅ Kakao Map SDK 초기화
        KakaoMapSdk.init(this, "306ea9db2d08a4684b5a8e110f9f0a4e")

    }
}




