package com.mp.matematch.main.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.mp.matematch.R
import com.mp.matematch.databinding.ActivityMainBinding

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

        // 2. NavController를 초기화합니다.
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // 3. userType에 따라 올바른 네비게이션 그래프(피드)를 설정합니다.
        when (userType) {
            // "Finder" (B그룹: 집 찾기)
            "HouseSeeker" -> {
                // "집 피드"가 시작 화면인 그래프를 설정합니다.
                navController.setGraph(R.navigation.nav_graph_house_seeker)
            }
            // "Provider" (A그룹) 또는 "Seeker" (C그룹)
            else -> {
                // "사람 피드"가 시작 화면인 그래프를 설정합니다.
                navController.setGraph(R.navigation.nav_graph_roommate_seeker)
            }
        }

        // 4. 하단 탭 바(BottomNavigationView)와 NavController를 연결합니다.
        NavigationUI.setupWithNavController(binding.bottomNavigationView, navController)
    }
}