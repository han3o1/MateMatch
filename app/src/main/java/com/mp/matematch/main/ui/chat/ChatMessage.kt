package com.mp.matematch.main.ui.chat

data class ChatMessage(
    val senderUid: String = "",
    val receiverUid: String = "",
    val message: String = "",
    val timestamp: Long = 0L
)
