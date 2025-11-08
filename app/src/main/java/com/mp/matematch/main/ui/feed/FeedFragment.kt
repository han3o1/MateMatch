package com.mp.matematch.main.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mp.matematch.R

class FeedFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_feed, container, false)

        // 온보딩이나 로그인에서 전달된 userType 값 가져오기
        val userType = requireActivity().intent.getStringExtra("userType")

        // ✅ 기존 피드 코드 그대로 살려두고, 단순히 분기만 담당
        val targetFragment: Fragment = if (userType == "provider" || userType == "roommate-provider") {
            FeedHouseFragment()   // 👉 그대로 사용
        } else {
            FeedPersonalFragment() // 👉 그대로 사용
        }

        childFragmentManager.beginTransaction()
            .replace(R.id.feedContainer, targetFragment)
            .commit()

        return view
    }
}
