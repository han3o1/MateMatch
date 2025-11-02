package com.mp.matematch.main.ui.feed

data class Person(
    val name: String,
    val age: Int,
    val job: String,
    val location: String,
    val rentRange: String,
    val description: String,
    val tags: List<String>,
    val profileImageResId: Int // drawable 이미지 리소스 ID
)