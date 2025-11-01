package com.mp.matematch

import android.app.Application
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize

class MateMatchApplication : Application() { // 👈 이 부분이 중요합니다.
    override fun onCreate() {
        super.onCreate()
        // 앱이 시작될 때 Firebase를 초기화합니다.
        Firebase.initialize(this)
    }
}