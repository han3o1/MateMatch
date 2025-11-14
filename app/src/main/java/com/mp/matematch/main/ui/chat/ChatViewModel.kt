package com.mp.matematch.main.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions


import com.google.firebase.firestore.Query
import com.mp.matematch.main.ui.chat.ChatMessage



class ChatViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val currentUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages

    private var listenerRegistration: ListenerRegistration? = null

    fun loadMessages(receiverUid: String) {
        val chatId = getChatId(currentUid, receiverUid)

        listenerRegistration?.remove() // 중복 방지
        listenerRegistration = db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val msgList = snapshot.documents.mapNotNull { it.toObject(Message::class.java) }
                _messages.value = msgList
            }
    }

    fun sendMessage(receiverUid: String, text: String) {
        if (text.isBlank()) return

        val myUid = currentUid
        val chatId = getChatId(myUid, receiverUid)

        val message = Message(
            senderId = myUid,
            text = text,
            timestamp = System.currentTimeMillis()
        )

        // 1️⃣ 메시지 저장
        db.collection("chats")
            .document(chatId)
            .collection("messages")
            .add(message)
            .addOnSuccessListener {
                // 2️⃣ chatList 업데이트 (너한테 없던 기능)
                updateChatList(chatId, myUid, receiverUid, text)
            }
    }


    private fun getChatId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_$uid2" else "${uid2}_$uid1"
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }

    private fun updateChatList(chatId: String, myUid: String, otherUid: String, lastMessage: String) {

        val myRoomData = mapOf(
            "chatId" to chatId,
            "uid" to otherUid,
            "lastMessage" to lastMessage,
            "timestamp" to FieldValue.serverTimestamp(),
            "hasNewMessage" to false
        )

        val otherRoomData = mapOf(
            "chatId" to chatId,
            "uid" to myUid,
            "lastMessage" to lastMessage,
            "timestamp" to FieldValue.serverTimestamp(),
            "hasNewMessage" to true
        )

        // 1) 내 chatList
        db.collection("chatList")
            .document(myUid)
            .collection("rooms")
            .document(chatId)
            .set(myRoomData, SetOptions.merge())

        // 2) 상대방 chatList
        db.collection("chatList")
            .document(otherUid)
            .collection("rooms")
            .document(chatId)
            .set(otherRoomData, SetOptions.merge())
    }

}

