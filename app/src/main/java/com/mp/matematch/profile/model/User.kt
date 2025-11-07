package com.mp.matematch.profile.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

@Keep
data class User(

    @get:PropertyName("uid")
    @set:PropertyName("uid")
    var uid: String? = "",

    @get:PropertyName("name")
    @set:PropertyName("name")
    var name: String? = "",

    @get:PropertyName("age")
    @set:PropertyName("age")
    var age: Int? = 0,

    @get:PropertyName("gender")
    @set:PropertyName("gender")
    var gender: String? = "",

    @get:PropertyName("job")
    @set:PropertyName("job")
    var job: String? = "",

    @get:PropertyName("location")
    @set:PropertyName("location")
    var location: String? = "",

    @get:PropertyName("rentRange")
    @set:PropertyName("rentRange")
    var rentRange: String? = "",

    @get:PropertyName("description")
    @set:PropertyName("description")
    var description: String? = "",

    @get:PropertyName("tags")
    @set:PropertyName("tags")
    var tags: List<String>? = emptyList(),

    @get:PropertyName("profileImageUrl")
    @set:PropertyName("profileImageUrl")
    var profileImageUrl: String? = "",

    @get:PropertyName("createdAt")
    @set:PropertyName("createdAt")
    var createdAt: Timestamp? = null,

    @get:PropertyName("updatedAt")
    @set:PropertyName("updatedAt")
    var updatedAt: Timestamp? = null
) {

    /** Firestore 업로드용 Map 변환 함수 */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "uid" to uid,
            "name" to name,
            "age" to age,
            "gender" to gender,
            "job" to job,
            "location" to location,
            "rentRange" to rentRange,
            "description" to description,
            "tags" to tags,
            "profileImageUrl" to profileImageUrl,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }

    /** 필드 업데이트용 copy 함수 (optional) */
    fun updateProfile(
        name: String? = this.name,
        age: Int? = this.age,
        job: String? = this.job,
        location: String? = this.location,
        description: String? = this.description,
        tags: List<String>? = this.tags,
        imageUrl: String? = this.profileImageUrl
    ): User {
        return this.copy(
            name = name,
            age = age,
            job = job,
            location = location,
            description = description,
            tags = tags,
            profileImageUrl = imageUrl,
            updatedAt = Timestamp.now()
        )
    }
}
