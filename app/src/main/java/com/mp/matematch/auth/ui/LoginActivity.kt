package com.mp.matematch.auth.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.mp.matematch.main.ui.MainActivity
import com.mp.matematch.databinding.ActivityLoginBinding
import com.mp.matematch.purpose.ui.PurposeSelectionActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val authViewModel: AuthViewModel by viewModels() // ViewModel 주입
    // private lateinit var auth: FirebaseAuth // ViewModel이 관리하므로 삭제

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // auth = FirebaseAuth.getInstance() // ViewModel이 관리하므로 삭제

        // 1. 로그인 버튼 클릭
        binding.btnSignIn.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                authViewModel.login(email, password) // ViewModel에 작업 요청
            } else {
                Toast.makeText(this, "이메일과 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, "로그인 성공!", Toast.LENGTH_SHORT).show()

                // TODO: 프로필이 이미 작성되었는지 확인하는 로직 필요
                // (지금은 임시로 '이용 목적 선택'으로 이동)
                val intent = Intent(this, PurposeSelectionActivity::class.java)
                startActivity(intent)
                finish() // 로그인 화면 종료
            }

            // 에러 상태
            state.error?.let {
                Toast.makeText(this, "로그인 실패: $it", Toast.LENGTH_LONG).show()
            }
        }
    }
}