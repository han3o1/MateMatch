package com.mp.matematch.main.ui.chat

data class Message(
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = 0L,

    val audioUrl: String? = null
)
