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
import com.mp.matematch.databinding.FragmentFeedHouseBinding
import com.mp.matematch.main.ui.chat.ChatRoomActivity

class FeedHouseFragment : Fragment() {

    private var _binding: FragmentFeedHouseBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FeedViewModel by viewModels()
    private lateinit var houseAdapter: HouseAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedHouseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        houseAdapter = HouseAdapter(mutableListOf()) { partnerUid ->
            if (partnerUid != null) {
                startChat(partnerUid)
            } else {
                Toast.makeText(requireContext(), "Error: Could not find user", Toast.LENGTH_SHORT).show()
                Log.e("FeedHouseFragment", "사용자를 찾을 수 없습니다.")
            }
        }

        binding.recyclerViewHouse.apply {
            adapter = houseAdapter
            layoutManager = LinearLayoutManager(context)
        }

        viewModel.houseList.observe(viewLifecycleOwner, Observer { feedItems ->
            houseAdapter.updateData(feedItems)
            Log.d("FeedHouse", "피드 UI 업데이트: ${feedItems.size}개")
        })

        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            if (isLoading) {
                Log.d("FeedHouse", "집 피드 로딩 중...")
            }
        })

        viewModel.error.observe(viewLifecycleOwner, Observer { errorMessage ->
            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
            Log.e("FeedHouse", "ViewModel 오류: $errorMessage")
        })

        viewModel.loadHouseFeed()

        binding.searchBoxHouse.setOnClickListener {
            FilterDialog(requireContext()) { filters ->
                viewModel.applyHouseFilters(filters)
            }.showStep1()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ✅ 기존 함수 유지, 기능만 확장
    private fun startChat(partnerUid: String) {
        val currentUid = FirebaseAuth.getInstance().uid ?: return
        val chatId = listOf(currentUid, partnerUid).sorted().joinToString("_")

        val db = FirebaseFirestore.getInstance()
        val chatRef = db.collection("chats").document(chatId)

        chatRef.get().addOnSuccessListener { document ->
            if (!document.exists()) {
                val chatData = mapOf(
                    "participants" to listOf(currentUid, partnerUid),
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "lastMessage" to ""
                )

                chatRef.set(chatData).addOnSuccessListener {
                    Log.d("FeedHouseFragment", "채팅방 생성 후 이동: $chatId")
                    moveToChat(chatId, partnerUid)
                }
            } else {
                Log.d("FeedHouseFragment", "채팅방 존재 → 바로 이동: $chatId")
                moveToChat(chatId, partnerUid)
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "채팅방 조회 실패", Toast.LENGTH_SHORT).show()
            Log.e("FeedHouseFragment", "Firestore 오류: ${it.message}")
        }
    }

    // ✅ ChatRoomActivity 연결용 Intent
    private fun moveToChat(chatId: String, partnerUid: String) {
        val intent = Intent(requireContext(), ChatRoomActivity::class.java)
        intent.putExtra("chatId", chatId)
        intent.putExtra("receiverUid", partnerUid)  // ChatRoomActivity에서 receiverUid로 받음
        startActivity(intent)
    }
}
