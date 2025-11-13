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

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. 로그인 버튼 클릭
        binding.btnSignIn.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                authViewModel.login(email, password)
            } else {
                Toast.makeText(this, "Enter your email and password.", Toast.LENGTH_SHORT).show()
            }
        }

        // 2. 회원가입 텍스트 클릭 (SignUpActivity로 이동)
        binding.tvSignUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        // 3. ViewModel의 상태(Livedata) 관찰
        observeLoginState()
    }

    private fun observeLoginState() {
        authViewModel.loginState.observe(this) { state ->
            // 로딩 상태
            binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

            // 성공 상태
            if (state.isSuccess) {
                Toast.makeText(this, "Logged in", Toast.LENGTH_SHORT).show()

                if (state.isNewUser == true) {
                    val intent = Intent(this, PurposeSelectionActivity::class.java)
                    startActivity(intent)
                } else {
                    val intent = Intent(this, MainActivity::class.java)
                    // (TODO: MainActivity가 userType을 필요로 한다면, Firestore에서 userType을 읽어와서 intent에 담아줘야 함)
                    startActivity(intent)
                }
                finishAffinity()
            }

            // 에러 상태
            state.error?.let {
                Toast.makeText(this, "Log in error: $it", Toast.LENGTH_LONG).show()
            }
        }
    }
}