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
import com.google.firebase.firestore.SetOptions
import com.mp.matematch.databinding.FragmentFeedPersonBinding
import com.mp.matematch.main.ui.chat.ChatRoomActivity
import com.mp.matematch.settings.SettingsRepository
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


        personAdapter = PersonAdapter(mutableListOf<FeedItem>()) { partnerUid ->
            if (partnerUid != null) {
                startChat(partnerUid)
            } else {
                Toast.makeText(requireContext(), "Error: Could not find user", Toast.LENGTH_SHORT).show()
                Log.e("FeedPersonFragment", "사용자를 찾을 수 없습니다.")
            }
        }

        setupRecyclerView()
        observeViewModel()
        setupListeners()
    }

    /**
     * 1. RecyclerView 뷰 모드 설정 함수
     */
    private fun setupRecyclerView() {
        // SettingsRepository에서 현재 뷰 모드 읽기
        val settingsRepo = SettingsRepository
        val currentViewMode = settingsRepo.getFeedViewMode(requireContext())

        // 뷰 모드에 따라 LayoutManager 동적 변경
        binding.recyclerViewPerson.apply {
            adapter = personAdapter
            layoutManager = if (currentViewMode == SettingsRepository.VIEW_MODE_CARD) {
                // '카드 뷰'일 때
                GridLayoutManager(requireContext(), 2)
            } else {
                // '리스트 뷰'일 때
                LinearLayoutManager(requireContext())
            }
        }
    }

    /**
     * 2. ViewModel 관찰자 설정 함수
     */
    private fun observeViewModel() {
        // '사람 목록'이 변경되면 어댑터에 데이터 전달
        viewModel.personList.observe(viewLifecycleOwner, Observer { feedItems ->
            personAdapter.updateData(feedItems)
            Log.d("FeedPerson", "피드 UI 업데이트: ${feedItems.size}개")
        })

        // (로딩/에러 관찰자 - 기존 코드)
        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            if(isLoading) {
                Log.d("FeedPerson", "사람 피드 로딩 중...")
            }
        })

        viewModel.error.observe(viewLifecycleOwner, Observer { errorMessage ->
            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
            Log.e("FeedPerson", "ViewModel 오류: $errorMessage")
        })

        viewModel.currentFilters.observe(viewLifecycleOwner, Observer { (city, building) ->
            Log.d("FeedPerson", "필터 감지: $city, $building. 피드 로드 시작.")
            viewModel.loadPersonFeed()
        })
    }

    /**
     * 3. 필터 리스너 설정 함수
     */
    private fun setupListeners() {
        // 필터 다이얼로그
        binding.searchBoxPerson.setOnClickListener {
            val dialog = FilterDialog(requireContext()) { filters ->
                val city = filters["city"] as? String ?: ""
                val buildingType = filters["buildingType"] as? String ?: "" // (사람 필터는 이 값을 무시할 수 있음)

                viewModel.applyFilter(city, buildingType)
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
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val chatId = listOf(currentUid, partnerUid).sorted().joinToString("_")

        val db = FirebaseFirestore.getInstance()
        val chatRef = db.collection("chats").document(chatId)

        chatRef.get().addOnSuccessListener { document ->
            if (!document.exists()) {
                // 🔥 채팅방 처음 만드는 경우
                val chatData = mapOf(
                    "participants" to listOf(currentUid, partnerUid),
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "lastMessage" to ""
                )

                chatRef.set(chatData).addOnSuccessListener {
                    Log.d("FeedPersonFragment", "채팅방 생성 후 이동: $chatId")
                    moveToChat(chatId, partnerUid)
                }
            } else {
                // ✅ 이미 존재하는 채팅방
                Log.d("FeedPersonFragment", "채팅방 존재 → 바로 이동: $chatId")
                moveToChat(chatId, partnerUid)
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "채팅방 조회 실패", Toast.LENGTH_SHORT).show()
            Log.e("FeedPersonFragment", "Firestore 오류: ${it.message}")
        }
    }
    private fun moveToChat(chatId: String, partnerUid: String) {
        val intent = Intent(requireContext(), ChatRoomActivity::class.java)
        intent.putExtra("chatId", chatId)
        intent.putExtra("receiverUid", partnerUid)
        startActivity(intent)
    }


}
