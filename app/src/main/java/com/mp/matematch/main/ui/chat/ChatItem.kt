package com.mp.matematch.main.ui.chat

data class ChatItem(
    val uid: String,
    val name: String,
    val job: String,
    val lastMessage: String,
    val timestamp: String,
    val profileImageUrl: String,
    val hasNewMessage: Boolean
)
