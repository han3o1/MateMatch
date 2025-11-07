package com.mp.matematch.main.ui.feed

data class House(
    val title: String,
    val price: String,
    val location: String,
    val description: String,
    val tags: List<String>,
    val imageResId: Int,
    val moveInDate: String, // ← 이거 추가됨
    val roomType: String    // ← 이것도
)

