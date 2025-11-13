package com.mp.matematch.main.ui.feed

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedPersonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ★ House와 동일하게 → 메시지 콜백 포함한 Adapter 생성
        personAdapter = PersonAdapter(mutableListOf()) { partnerUid ->
            startChat(partnerUid)
        }

        binding.recyclerViewPerson.apply {
            adapter = personAdapter
            layoutManager = LinearLayoutManager(context)
        }

        viewModel.personList.observe(viewLifecycleOwner, Observer { people ->
            personAdapter.updateData(people)
            Log.d("FeedPerson", "피드 UI 업데이트: ${people.size}개")
        })

        viewModel.loadPersonFeed()

        // 필터 다이얼로그
        binding.searchBoxPerson.setOnClickListener {
            FilterDialog(requireContext()) { filters ->
                viewModel.applyPersonFilters(filters)
            }.showStep1()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ★ 채팅방 생성 + 이동 함수
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
