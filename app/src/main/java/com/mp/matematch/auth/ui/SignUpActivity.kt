package com.mp.matematch.auth.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.mp.matematch.R
import com.mp.matematch.databinding.ActivitySignupBinding
import com.mp.matematch.main.ui.MainActivity
import com.mp.matematch.purpose.ui.PurposeSelectionActivity

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private val authViewModel: AuthViewModel by viewModels() // ViewModel 주입
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                // 구글 로그인 성공 -> Firebase에 인증 요청
                val account = task.getResult(ApiException::class.java)!!

                account.idToken?.let { token ->
                    authViewModel.firebaseAuthWithGoogle(token)
                } ?: run {
                    Toast.makeText(this, "Failed to get Google ID token.", Toast.LENGTH_SHORT).show()
                }

            } catch (e: ApiException) {
                // 구글 로그인 실패
                Toast.makeText(this, "Google sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.btnSignUp.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (email.isNotEmpty() && password.isNotEmpty()) {
                authViewModel.signUp(email, password) // ViewModel에 작업 요청
            } else {
                Toast.makeText(this, "Please enter your email and password.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnGoogleContinue.setOnClickListener {
            signInWithGoogle()
        }

        binding.tvSignIn.setOnClickListener {
            finish()
        }

        observeLoginState()
        observeSignUpState()
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun observeLoginState() {
        authViewModel.loginState.observe(this) { state ->
            // 로딩 상태
            binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

            // 성공 상태
            if (state.isSuccess) {
                Toast.makeText(this, "Logged in", Toast.LENGTH_SHORT).show()

                if (state.isNewUser == true) {
                    // 신규 유저 -> 프로필 설정(PurposeSelectionActivity)으로 이동
                    val intent = Intent(this, PurposeSelectionActivity::class.java)
                    startActivity(intent)
                } else {
                    // 기존 유저 -> 메인 화면(MainActivity)으로 이동
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                }
                finishAffinity()
            }

            // 에러 상태
            state.error?.let {
                Toast.makeText(this, "Sign-up Failed: $it", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun observeSignUpState() {
        authViewModel.signUpState.observe(this) { state ->
            // 로딩 상태
            binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

            // 성공 상태
            if (state.isSuccess) {
                Toast.makeText(this, "Account Created!", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, PurposeSelectionActivity::class.java)
                startActivity(intent)
                finishAffinity()
            }

            // 에러 상태
            state.error?.let {
                Toast.makeText(this, "Sign-up Failed: $it", Toast.LENGTH_LONG).show()
            }
        }
    }
}