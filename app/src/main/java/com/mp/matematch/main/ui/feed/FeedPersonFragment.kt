package com.mp.matematch.main.ui.feed

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.mp.matematch.databinding.FragmentFeedPersonBinding
import com.mp.matematch.main.ui.chat.ChatRoomActivity
import com.mp.matematch.profile.model.User
import com.mp.matematch.settings.SettingsRepository
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase



class FeedPersonFragment : Fragment() {

    private var _binding: FragmentFeedPersonBinding? = null
    private val binding get() = _binding!!

    private lateinit var personAdapter: PersonAdapter

    // 🔥 ViewModel 정상 가져오기
    private val viewModel: FeedViewModel by viewModels()

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
            if (partnerUid != null) startChat(partnerUid)
        }

        setupRecyclerView()
        setupListeners()

        // 🔥 FeedViewModel의 LiveData 관찰
        viewModel.personList.observe(viewLifecycleOwner) { items ->
            personAdapter.updateData(items)
        }

        // 초기 로딩
        viewModel.loadPersonFeed()
    }

    private fun setupRecyclerView() {
        val mode = SettingsRepository.getFeedViewMode(requireContext())
        binding.recyclerViewPerson.apply {
            adapter = personAdapter
            layoutManager =
                if (mode == SettingsRepository.VIEW_MODE_CARD)
                    GridLayoutManager(requireContext(), 2)
                else
                    LinearLayoutManager(requireContext())
        }
    }

    private fun setupListeners() {
        binding.searchBoxPerson.setOnClickListener {
            val dialog = FilterDialog(requireContext()) { filters ->
                val city = filters["city"] as? String ?: ""
                val building = filters["buildingType"] as? String ?: ""
                viewModel.applyFilter(city, building)
                viewModel.loadPersonFeed()
            }
            dialog.showStep1()
        }
    }

    // -----------------------------
    // 🔥 채팅방 생성
    // -----------------------------
    private fun startChat(partnerUid: String) {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val chatId = listOf(currentUid, partnerUid).sorted().joinToString("_")

        val db = Firebase.firestore
        val ref = db.collection("chats").document(chatId)

        ref.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                val data = mapOf(
                    "participants" to listOf(currentUid, partnerUid),
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "lastMessage" to ""
                )
                ref.set(data).addOnSuccessListener {
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
