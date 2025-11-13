package com.mp.matematch.main.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mp.matematch.R
import com.mp.matematch.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // NavHostFragment 연결
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // ⭐ 앱 최초 진입 시 Firestore에서 userType 읽어서 올바른 그래프로 분기
        setupNavigationByUserType()

        // BottomNavigation 과 NavController 연결
        NavigationUI.setupWithNavController(binding.bottomNavigationView, navController)

        // 프로필 이미지 로드
        loadProfileImage()
    }

    /**
     * 🔥 가장 중요한 함수
     * 인텐트가 아니라 Firestore에서 userType을 읽어서
     * HouseSeeker → 집 피드 그래프
     * Provider, RoommateSeeker → 사람 피드 그래프
     * 로 완전히 분기해주는 함수
     */
    private fun setupNavigationByUserType() {
        val uid = FirebaseAuth.getInstance().uid ?: return

        FirebaseFirestore.getInstance().collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val type = doc.getString("userType") ?: ""
                Log.d("MainActivity", "Firestore userType: $type")

                val args = Bundle().apply {
                    putString("USER_TYPE", type)
                }

                when (type) {
                    "HouseSeeker" -> {
                        Log.d("MainActivity", "➡ HouseSeeker → 집 피드로 이동")
                        navController.setGraph(R.navigation.nav_graph_house_seeker, args)
                    }

                    "Provider", "RoommateSeeker" -> {
                        Log.d("MainActivity", "➡ Provider/Roommate → 사람 피드로 이동")
                        navController.setGraph(R.navigation.nav_graph_roommate_seeker, args)
                    }

                    else -> {
                        Log.e("MainActivity", "Unknown userType=$type → 기본 사람 피드로 이동")
                        navController.setGraph(R.navigation.nav_graph_roommate_seeker, args)
                    }
                }
            }
            .addOnFailureListener {
                Log.e("MainActivity", "userType 불러오기 실패 → 기본 사람피드로 이동", it)
                navController.setGraph(R.navigation.nav_graph_roommate_seeker)
            }
    }

    /**
     * Firestore에서 프로필 이미지 가져와서 하단 탭에 세팅
     */
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
