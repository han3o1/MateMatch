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
import com.mp.matematch.databinding.ActivityLoginBinding
import com.mp.matematch.main.ui.MainActivity
import com.mp.matematch.purpose.ui.PurposeSelectionActivity
import android.content.pm.PackageManager
import android.util.Base64
import android.util.Log
import java.security.MessageDigest

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val authViewModel: AuthViewModel by viewModels()

    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!

                account.idToken?.let { token ->
                    authViewModel.firebaseAuthWithGoogle(token)
                } ?: run {
                    Toast.makeText(this, "Failed to get Google ID token.", Toast.LENGTH_SHORT).show()
                }

            } catch (e: ApiException) {
                Toast.makeText(this, "Google login failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Kakao 해시키 출력
        printKakaoHashKey()

        // Google 로그인 Init
        setupGoogleLogin()

        // 이메일 로그인 버튼
        binding.btnSignIn.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                authViewModel.login(email, password)
            } else {
                Toast.makeText(this, "Enter your email and password.", Toast.LENGTH_SHORT).show()
            }
        }

        // Google 로그인 버튼
        binding.btnGoogleLogin.setOnClickListener {
            signInWithGoogle()
        }

        // 회원가입 이동
        binding.tvSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        observeLoginState()
    }

    /** 로그인 상태 옵저버 */
    private fun observeLoginState() {
        authViewModel.loginState.observe(this) { state ->

            binding.progressBar.visibility =
                if (state.isLoading) View.VISIBLE else View.GONE

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

    /** Google Sign-in Init */
    private fun setupGoogleLogin() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Firebase Web Client ID
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    /** Google Login */
    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    /** Kakao KeyHash 출력 */
    private fun printKakaoHashKey() {
        try {
            val packageInfo =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    packageManager.getPackageInfo(
                        packageName,
                        PackageManager.GET_SIGNING_CERTIFICATES
                    )
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getPackageInfo(
                        packageName,
                        PackageManager.GET_SIGNATURES
                    )
                }

            val signatures =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
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
