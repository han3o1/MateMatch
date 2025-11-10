package com.mp.matematch.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mp.matematch.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // NavHostFragment 연결
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // BottomNavigation 연결
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav.setupWithNavController(navController)

        // ✅ 추가: ProfileSetupEActivity에서 넘어온 userType 가져오기
        val userType = intent.getStringExtra("userType")

        // ✅ 초기 화면이 FeedFragment일 때만 arguments 전달
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.feedFragment && userType != null) {
                val bundle = Bundle().apply {
                    putString("userType", userType)
                }
                navController.navigate(R.id.feedFragment, bundle)
            }
        }

        // ✅ 초기 화면은 FeedFragment (FeedFragment 안에서 userType으로 하위 피드 자동 분기)
        if (navController.currentDestination?.id != R.id.feedFragment) {
            navController.navigate(R.id.feedFragment)
        }
    }
}
