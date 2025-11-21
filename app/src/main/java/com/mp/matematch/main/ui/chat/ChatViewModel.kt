package com.mp.matematch.main.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

class ChatViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val currentUid = FirebaseAuth.getInstance().currentUser!!.uid

    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages

    // 📌 메시지 불러오기 (실시간)
    fun loadMessages(chatId: String) {
        db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val list = snapshot.documents.map { doc ->
                        val msg = doc.toObject(Message::class.java)!!
                        msg.id = doc.id      // 🔥 문서 ID 저장
                        msg
                    }
                    _messages.value = list
                }
            }
    }


    // 📌 메시지 보내기 (현재 사용자 → 상대에게)
    fun sendMessage(chatId: String, text: String) {
        if (text.isBlank()) return

        val msg = mapOf(
            "senderId" to currentUid,
            "text" to text,
            "timestamp" to System.currentTimeMillis()
        )

        // 🔥 1. 메시지 저장
        db.collection("chats")
            .document(chatId)
            .collection("messages")
            .add(msg)

        // 🔥 2. chats/{chatId}의 lastMessage & updatedAt 업데이트
        db.collection("chats")
            .document(chatId)
            .update(
                mapOf(
                    "lastMessage" to text,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
    }
}
