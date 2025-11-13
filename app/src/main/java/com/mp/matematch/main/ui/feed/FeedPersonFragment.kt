package com.mp.matematch.main.ui.feed

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.mp.matematch.databinding.FragmentFeedPersonBinding
import com.mp.matematch.main.ui.chat.ChatRoomActivity
import com.mp.matematch.profile.model.User

class FeedPersonFragment : Fragment() {

    private var _binding: FragmentFeedPersonBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FeedViewModel by viewModels()
    private lateinit var personAdapter: PersonAdapter
    private var currentUserType: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedPersonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        personAdapter = PersonAdapter(mutableListOf<FeedItem>()) { partnerUid ->
            if (partnerUid != null) {
                startChat(partnerUid)
            } else {
                Toast.makeText(requireContext(), "Error: Could not find user", Toast.LENGTH_SHORT).show()
                Log.e("FeedPersonFragment", "사용자를 찾을 수 없습니다.")
            }
        }

        binding.recyclerViewPerson.apply {
            adapter = personAdapter
            layoutManager = LinearLayoutManager(context)
        }

        viewModel.personList.observe(viewLifecycleOwner, Observer { feedItems ->
            personAdapter.updateData(feedItems)
            Log.d("FeedPerson", "피드 UI 업데이트: ${feedItems.size}개")
        })

        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            if(isLoading) {
                Log.d("FeedPerson", "사람 피드 로딩 중...")
            }
        })

        viewModel.error.observe(viewLifecycleOwner, Observer { errorMessage ->
            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
            Log.e("FeedPerson", "ViewModel 오류: $errorMessage")
        })

        viewModel.loadPersonFeed()


        // 필터 다이얼로그
        binding.searchBoxPerson.setOnClickListener {
            val dialog = FilterDialog(requireContext()) { filters ->
                viewModel.applyPersonFilters(filters)
            }
            dialog.showStep1()

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // 채팅방 생성 / 이동 함수
    private fun startChat(partnerUid: String) {
        val currentUid = FirebaseAuth.getInstance().uid!!
        val chatId = if (currentUid < partnerUid)
            "${currentUid}_${partnerUid}"
        else
            "${partnerUid}_${currentUid}"

        val chatData = mapOf(
            "participants" to listOf(currentUid, partnerUid),
            "updatedAt" to FieldValue.serverTimestamp(),
            "lastMessage" to ""
        )

        val db = FirebaseFirestore.getInstance()

        db.collection("chats").document(chatId)
            .set(chatData, SetOptions.merge())
            .addOnSuccessListener {
                val intent = Intent(requireContext(), ChatRoomActivity::class.java)
                intent.putExtra("chatId", chatId)
                intent.putExtra("partnerUid", partnerUid)
                startActivity(intent)
            }
    }
}
