package com.mp.matematch.main.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mp.matematch.R

import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

// Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date





class ChatFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private val chatList = mutableListOf<ChatItem>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadUsersFromFirestore()   // ★ 여기로 이동
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chat, container, false)
        recyclerView = view.findViewById(R.id.recyclerViewChatList)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        chatAdapter = ChatAdapter(chatList)
        recyclerView.adapter = chatAdapter



        return view
    }

    override fun onResume() {
        super.onResume()
        loadUsersFromFirestore() // ✅ 채팅 탭 올 때마다 새로고침
    }

    private fun loadUsersFromFirestore() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("chats")
            .whereArrayContains("participants", currentUid)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                Log.d("ChatFragment", "✅ chats found = ${documents.size()}")

                if (documents.isEmpty) {
                    chatList.clear()
                    chatAdapter.notifyDataSetChanged()
                    updateEmptyState()
                    return@addOnSuccessListener
                }

                val tempList = mutableListOf<ChatItem>()   // ⭐ 임시 리스트

                for (doc in documents) {

                    val chatId = doc.id
                    val participants = doc.get("participants") as? List<String> ?: continue
                    val partnerUid = participants.firstOrNull { it != currentUid } ?: continue

                    val lastMessage = doc.getString("lastMessage") ?: ""
                    val timestampMillis = doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
                    val formattedTime = formatTimestamp(timestampMillis)

                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(partnerUid)
                        .get()
                        .addOnSuccessListener { userDoc ->
                            val name = userDoc.getString("name") ?: "Unknown"
                            val job = userDoc.getString("job") ?: ""
                            val profileImageUrl = userDoc.getString("profileImageUrl") ?: ""

                            val item = ChatItem(
                                chatId = chatId,
                                uid = partnerUid,
                                name = name,
                                job = job,
                                lastMessage = lastMessage,
                                timestamp = formattedTime,
                                profileImageUrl = profileImageUrl,
                                hasNewMessage = false
                            )

                            tempList.add(item)

                            // ⭐ 모든 채팅 상대 정보 로딩 완료되었을 때만 RecyclerView 업데이트
                            if (tempList.size == documents.size()) {
                                chatList.clear()
                                chatList.addAll(tempList)
                                chatAdapter.notifyDataSetChanged()
                                updateEmptyState()
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("ChatFragment", "🔥 Failed to load user info: $partnerUid", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ChatFragment", "Error fetching chats", e)
            }
    }



    private fun formatTimestamp(timeMillis: Long): String {
        val sdf = SimpleDateFormat("a hh:mm", Locale.getDefault())
        return sdf.format(Date(timeMillis))
    }


    private fun updateEmptyState() {
        val emptyView = view?.findViewById<View>(R.id.emptyView)
        if (chatList.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyView?.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyView?.visibility = View.GONE
        }
    }
}
