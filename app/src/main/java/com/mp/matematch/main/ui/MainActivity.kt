package com.mp.matematch.main.ui

import android.util.Log
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.mp.matematch.R
import com.mp.matematch.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    // (ProfileSetupEActivity에서 넘겨준 userType을 저장할 변수)
    var userType: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. 프로필 설정에서 넘겨받은 userType을 가져옵니다.
        userType = intent.getStringExtra("USER_TYPE")
        Log.d("MainActivity", "넘겨받은 userType: $userType")

        // ⭐ FEED 프래그먼트들에서 arguments로 받을 수 있게 Bundle 생성
        val bundle = Bundle().apply {
            putString("USER_TYPE", userType)
        }

        // 2. NavController를 초기화합니다.
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val args = Bundle().apply {
            putString("USER_TYPE", userType)
        }

        // 3. userType에 따라 올바른 네비게이션 그래프(피드)를 설정합니다.
        when (userType) {
            // "HouseSeeker" (B그룹: 집 찾기)
            "HouseSeeker" -> {
                // "집 피드"가 시작 화면인 그래프를 설정합니다.
                navController.setGraph(R.navigation.nav_graph_house_seeker,args)
            }
            // "Provider" (A그룹) 또는 "Seeker" (C그룹)
            else -> {
                // "사람 피드"가 시작 화면인 그래프를 설정합니다.
                navController.setGraph(R.navigation.nav_graph_roommate_seeker,args)
            }
        }

        // 4. 하단 탭 바(BottomNavigationView)와 NavController를 연결합니다.
        NavigationUI.setupWithNavController(binding.bottomNavigationView, navController)

        // ③ 프로필 이미지 로딩
        loadProfileImage()
    }

    private fun loadProfileImage() {
        val uid = FirebaseAuth.getInstance().uid ?: return

        FirebaseFirestore.getInstance().collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val url = doc.getString("profileImageUrl")

                Glide.with(this)
                    .load(url)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .into(binding.profileImageHouse)
            }
    }
}