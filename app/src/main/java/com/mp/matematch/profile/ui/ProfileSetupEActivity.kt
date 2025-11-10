package com.mp.matematch.profile.ui


import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.mp.matematch.databinding.ActivityProfileSetupEBinding
import com.mp.matematch.profile.viewmodel.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Intent
import com.mp.matematch.main.ui.MainActivity


class ProfileSetupEActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileSetupEBinding
    private val viewModel: ProfileViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileSetupEBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 🔹 기존 값 불러오기 (Firestore → UI)
        viewModel.user.observe(this) { user ->
            binding.inputStatus.setText(user.bio)
            binding.inputIntro.setText(user.tags.joinToString(", "))
        }

        // 🔹 뒤로가기
        binding.btnBack.setOnClickListener { finish() }

        // 🔹 "Complete Profile" 버튼 클릭
        binding.btnComplete.setOnClickListener {
            saveFinalProfile()
        }
    }

    private fun saveFinalProfile() {
        val status = binding.inputStatus.text?.toString()?.trim() ?: ""
        val intro = binding.inputIntro.text?.toString()?.trim() ?: ""



        // ✅ bio는 상태메시지로, tags는 쉼표로 구분된 리스트로 저장
        val tagList = if (intro.isNotEmpty()) {
            intro.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else emptyList()

        if (tagList.isEmpty()
        ) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Missing Required Fields")
                .setMessage("Please fill in all required fields (marked with * ) before proceeding to the next step.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        viewModel.updateField("bio", status)
        viewModel.updateField("tags", tagList)

        // ✅ Firestore 저장
        viewModel.saveUserProfile { success ->
            if (success) {
                Snackbar.make(binding.root, "Profile Updated!", Snackbar.LENGTH_LONG).show()
                goToMain()
            } else {
                Snackbar.make(binding.root, "Profile Update Fail", Snackbar.LENGTH_LONG).show()
            }
        }
    }

//    private fun goToMain() {
//        val intent = Intent(this, MainActivity::class.java)
//        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//        startActivity(intent)
//    }
private fun goToMain() {
    val currentUser = auth.currentUser ?: return

    // Firestore에서 userType 불러오기
    db.collection("users").document(currentUser.uid).get()
        .addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val userType = document.getString("userType") ?: ""

                // 🔹 userType 값을 MainActivity로 넘김
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("userType", userType)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            } else {
                Toast.makeText(this, "Cannot load user information.", Toast.LENGTH_SHORT).show()
            }
        }
        .addOnFailureListener {
            Toast.makeText(this, "Cannot load user type", Toast.LENGTH_SHORT).show()
        }
}


}
