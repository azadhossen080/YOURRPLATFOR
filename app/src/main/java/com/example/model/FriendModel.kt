package com.example.model

enum class FriendStatus {
    FRIEND,
    INCOMING_REQUEST,
    OUTGOING_REQUEST,
    SUGGESTION
}

data class FriendUser(
    val id: String,
    val name: String,
    val handle: String,
    val bio: String,
    val mutualFriendsCount: Int = 0,
    val avatarColorHex: Long = 0xFF5352ED,
    val isVerified: Boolean = false,
    val isOnline: Boolean = false,
    val status: FriendStatus = FriendStatus.SUGGESTION,
    val requestTimeAgo: String = ""
)
