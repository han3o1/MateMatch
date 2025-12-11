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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.mp.matematch.databinding.FragmentFeedHouseBinding
import com.mp.matematch.main.ui.chat.ChatRoomActivity
import com.mp.matematch.settings.SettingsRepository

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
            if (partnerUid != null) startChat(partnerUid)
            else Toast.makeText(requireContext(), "Error: Could not find user", Toast.LENGTH_SHORT).show()
        }

        setupRecyclerView()
        observeViewModel()
        setupListeners()

        viewModel.loadHouseFeed()
    }

    private fun setupRecyclerView() {
        val mode = SettingsRepository.getFeedViewMode(requireContext())

        binding.recyclerViewHouse.apply {
            adapter = houseAdapter
            layoutManager =
                if (mode == SettingsRepository.VIEW_MODE_CARD)
                    GridLayoutManager(requireContext(), 2)
                else
                    LinearLayoutManager(requireContext())
        }
    }

    private fun observeViewModel() {
        viewModel.houseList.observe(viewLifecycleOwner) { feedItems ->
            houseAdapter.updateData(feedItems)
            Log.d("FeedHouse", "피드 업데이트: ${feedItems.size}개")
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            if (loading) Log.d("FeedHouse", "로딩 중..")
        }

        viewModel.error.observe(viewLifecycleOwner) { msg ->
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupListeners() {
        binding.searchBoxHouse.setOnClickListener {
            val dialog = FilterDialog(requireContext()) { filters ->
                // FeedViewModel에서 모든 필터 관리함
                viewModel.applyFilters(filters)
            }
            dialog.showStep1()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ===========================================================
    // 🔥 채팅 기능 (정상 위치)
    // ===========================================================
    private fun startChat(partnerUid: String) {
        val currentUid = FirebaseAuth.getInstance().uid ?: return
        val chatId = listOf(currentUid, partnerUid).sorted().joinToString("_")

        val db = FirebaseFirestore.getInstance()
        val ref = db.collection("chats").document(chatId)

        ref.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                val chatData = mapOf(
                    "participants" to listOf(currentUid, partnerUid),
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "lastMessage" to ""
                )
                ref.set(chatData).addOnSuccessListener {
                    moveToChat(chatId, partnerUid)
                }
            } else {
                moveToChat(chatId, partnerUid)
            }
        }
    }

    private fun moveToChat(chatId: String, partnerUid: String) {
        val intent = Intent(requireContext(), ChatRoomActivity::class.java)
        intent.putExtra("chatId", chatId)
        intent.putExtra("receiverUid", partnerUid)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
