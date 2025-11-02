package com.mp.matematch.main // ← 네 패키지 경로에 맞게 수정!

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mp.matematch.R
import com.mp.matematch.main.ui.feed.FeedPersonalFragment


class TestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        supportFragmentManager.beginTransaction()
            .replace(R.id.testFragmentContainer, FeedPersonalFragment())
            .commit()
    }
}
