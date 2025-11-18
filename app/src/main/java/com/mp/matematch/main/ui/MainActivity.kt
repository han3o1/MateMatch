package com.mp.matematch.main.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mp.matematch.R
import com.mp.matematch.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private var isNavGraphReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        setupNavigationByUserType()

        binding.btnSettings.setOnClickListener {
            if (isNavGraphReady) {
                navController.navigate(R.id.action_global_to_settings)
            } else {
                Toast.makeText(this, "Loading user data...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupNavigationByUserType() {
        val uid = FirebaseAuth.getInstance().uid

        if (uid == null) {
            Log.e("MainActivity", "User is logged out! Setting default graph.")
            navController.setGraph(R.navigation.nav_graph_roommate_seeker)
            NavigationUI.setupWithNavController(binding.bottomNavigationView, navController)
            isNavGraphReady = true

            setupDestinationListener()
            return
        }

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
                        navController.setGraph(R.navigation.nav_graph_house_seeker, args)
                    }
                    "Provider", "RoommateSeeker" -> {
                        navController.setGraph(R.navigation.nav_graph_roommate_seeker, args)
                    }
                    else -> {
                        navController.setGraph(R.navigation.nav_graph_roommate_seeker, args)
                    }
                }

                NavigationUI.setupWithNavController(binding.bottomNavigationView, navController)
                isNavGraphReady = true

                setupDestinationListener()
            }
            .addOnFailureListener {
                Log.e("MainActivity", "userType 불러오기 실패 → 기본 사람피드로 이동", it)

                navController.setGraph(R.navigation.nav_graph_roommate_seeker)
                NavigationUI.setupWithNavController(binding.bottomNavigationView, navController)
                isNavGraphReady = true

                setupDestinationListener()
            }
    }

    private fun setupDestinationListener() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.settingsFragment) {
                binding.bottomNavigationView.visibility = View.GONE
            } else {
                binding.bottomNavigationView.visibility = View.VISIBLE
            }
        }
    }
}