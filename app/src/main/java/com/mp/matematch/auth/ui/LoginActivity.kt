package com.mp.matematch.auth.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.mp.matematch.databinding.ActivityLoginBinding
import com.mp.matematch.purpose.ui.PurposeSelectionActivity
import com.mp.matematch.main.ui.MainActivity
import android.content.pm.PackageManager
import android.util.Base64
import android.util.Log
import java.security.MessageDigest

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        printKakaoHashKey() // ✅ Kakao Developers 등록용 키 해시 확인용

        // ✅ 로그인 버튼 클릭
        binding.btnSignIn.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                authViewModel.login(email, password)
            } else {
                Toast.makeText(this, "Enter your email and password.", Toast.LENGTH_SHORT).show()
            }
        }

        // ✅ 회원가입 버튼 클릭
        binding.tvSignUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        // ✅ 로그인 상태 관찰
        observeLoginState()
    }

    private fun observeLoginState() {
        authViewModel.loginState.observe(this) { state ->
            binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

            if (state.isSuccess) {
                Toast.makeText(this, "Logged in", Toast.LENGTH_SHORT).show()

                val nextActivity = if (state.isNewUser == true) {
                    PurposeSelectionActivity::class.java
                } else {
                    MainActivity::class.java
                }

                startActivity(Intent(this, nextActivity))
                finishAffinity()
            }

            state.error?.let {
                Toast.makeText(this, "Log in error: $it", Toast.LENGTH_LONG).show()
            }
        }
    }

    /** ✅ Kakao Developers 등록용 해시 키 출력 함수 **/
    private fun printKakaoHashKey() {
        try {
            val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            }

            val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            signatures?.forEach { signature ->
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val keyHash = Base64.encodeToString(md.digest(), Base64.DEFAULT)
                Log.e("🔑 Kakao Key Hash", keyHash)
            } ?: Log.e("🔑 Kakao Key Hash", "No signatures found.")
        } catch (e: Exception) {
            Log.e("🔑 Kakao Key Hash", "Error getting KeyHash", e)
        }
    }
}
