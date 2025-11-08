package com.mp.matematch.core.navigation

import android.content.Context
import android.content.Intent
import com.mp.matematch.main.MainActivity

object NavigationUtils {

    /**
     * 온보딩 완료 후 MainActivity로 이동.
     * userType("provider" / "houseSeeker") 값을 함께 전달.
     */
    fun navigateToMain(context: Context, userType: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("userType", userType)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(intent)
    }
}
