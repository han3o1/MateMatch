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

    fun loadMessages(chatId: String) {
        db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Message::class.java)?.apply {
                            id = doc.id   // 🔥 여기서 Firestore 문서 ID 넣음
                        }
                    }
                    _messages.value = list
                }
            }
    }

    fun sendMessage(chatId: String, text: String) {
        if (text.isBlank()) return

        val msg = mapOf(
            "senderId" to currentUid,
            "text" to text,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("chats")
            .document(chatId)
            .collection("messages")
            .add(msg)

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

