package com.mp.matematch.main.ui.chat

data class Message(
    var id: String = "",
    val senderId: String = "",
    val text: String = "",
    val audioUrl: String? = null,
    val timestamp: Long = 0L
)
