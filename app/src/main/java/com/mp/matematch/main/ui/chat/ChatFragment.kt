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




class ChatFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private val chatList = mutableListOf<ChatItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chat, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        chatAdapter = ChatAdapter(chatList)
        recyclerView.adapter = chatAdapter

        loadUsersFromFirestore()

        return view
    }

    private fun loadUsersFromFirestore() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid

        FirebaseFirestore.getInstance().collection("users")
            .get()
            .addOnSuccessListener { documents ->
                chatList.clear()
                for (doc in documents) {
                    val uid = doc.getString("uid") ?: continue
                    if (uid == currentUid) continue  // 자기 자신 제외

                    val name = doc.getString("name") ?: "Unknown"
                    val job = doc.getString("job") ?: ""
                    val profileImageUrl = doc.getString("profileImageUrl") ?: ""
                    val lastMessage = doc.getString("statusMessage") ?: ""
                    val timestamp = "2h ago" // 예시 (실제 채팅 DB 있다면 갱신 가능)

                    val item = ChatItem(
                        uid = uid,
                        name = name,
                        job = job,
                        lastMessage = lastMessage,
                        timestamp = timestamp,
                        profileImageUrl = profileImageUrl,
                        hasNewMessage = true // 임시 처리. 추후 실시간 여부로 제어 가능
                    )
                    chatList.add(item)
                }
                chatAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("ChatFragment", "Error fetching users", e)
            }
    }
}
