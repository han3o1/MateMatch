package com.mp.matematch.auth.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.mp.matematch.databinding.ActivitySignupBinding
import com.mp.matematch.purpose.ui.PurposeSelectionActivity

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private val authViewModel: AuthViewModel by viewModels() // ViewModel 주입

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. 회원가입 버튼 클릭
        binding.btnSignUp.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            if (password != confirmPassword) {
                Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (email.isNotEmpty() && password.isNotEmpty()) {
                authViewModel.signUp(email, password) // ViewModel에 작업 요청
            } else {
                Toast.makeText(this, "이메일과 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }

        // 2. 이미 계정이 있으신가요? (로그인 화면으로 이동)
        binding.tvSignIn.setOnClickListener {
            finish() // 현재 회원가입 화면 종료
        }

        // 3. ViewModel의 상태(Livedata) 관찰
        observeSignUpState()
    }

    private fun observeSignUpState() {
        authViewModel.signUpState.observe(this) { state ->
            // 로딩 상태에 따라 프로그레스바 표시
            binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

            // 성공 상태
            if (state.isSuccess) {
                Toast.makeText(this, "회원가입 성공!", Toast.LENGTH_SHORT).show()
                // 다음 단계(이용 목적 선택)로 이동
                val intent = Intent(this, PurposeSelectionActivity::class.java)
                startActivity(intent)
                finishAffinity() // 이전의 모든 화면(로그인 등)을 종료
            }

            // 에러 상태
            state.error?.let {
                Toast.makeText(this, "회원가입 실패: $it", Toast.LENGTH_LONG).show()
            }
        }
    }
}