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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.mp.matematch.databinding.FragmentFeedPersonBinding
import com.mp.matematch.main.ui.chat.ChatRoomActivity
import com.mp.matematch.settings.SettingsRepository
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch


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

        personAdapter = PersonAdapter(mutableListOf()) { partnerUid ->
            if (partnerUid != null) {
                startChat(partnerUid)
            } else {
                Toast.makeText(requireContext(), "Error: Could not find user", Toast.LENGTH_SHORT).show()
            }
        }

        setupRecyclerView()
        observeViewModel()
        setupListeners()

        viewModel.loadPersonFeed()

    }

    private fun setupRecyclerView() {
        val settingsRepo = SettingsRepository
        val mode = settingsRepo.getFeedViewMode(requireContext())

        binding.recyclerViewPerson.apply {
            adapter = personAdapter
            layoutManager =
                if (mode == SettingsRepository.VIEW_MODE_CARD)
                    GridLayoutManager(requireContext(), 2)
                else
                    LinearLayoutManager(requireContext())
        }
    }

    private fun observeViewModel() {

        viewModel.personList.observe(viewLifecycleOwner, Observer { feedItems ->
            personAdapter.updateData(feedItems)
            Log.d("FeedPerson", "피드 업데이트: ${feedItems.size}개")
        })

        viewModel.isLoading.observe(viewLifecycleOwner, Observer { loading ->
            if (loading) Log.d("FeedPerson", "로딩 중..")
        })

        viewModel.error.observe(viewLifecycleOwner, Observer { msg ->
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        })

        // 기존 필터 Pair(city,buildingType)는 더 이상 사용 안 함
        // viewModel.currentFilters.observe {...} 제거해도 됨
    }

    private fun setupListeners() {
        binding.searchBoxPerson.setOnClickListener {
            val dialog = FilterDialog(requireContext()) { filters ->
                //  FeedViewModel이 모든 필터를 처리함
                viewModel.applyFilters(filters)
            }
            dialog.showStep1()
        }
    }

        override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun startChat(partnerUid: String) {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
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

}
