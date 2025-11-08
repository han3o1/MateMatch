package com.mp.matematch.main.ui

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

        // ✅ 초기 화면은 FeedFragment (FeedFragment 안에서 userType으로 하위 피드 자동 분기)
        if (navController.currentDestination?.id != R.id.feedFragment) {
            navController.navigate(R.id.feedFragment)
        }
    }
}
