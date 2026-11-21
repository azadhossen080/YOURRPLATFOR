package com.example.model

data class UserProfile(
    val name: String = "Guest User",
    val userId: String = "",
    val handle: String = "",
    val bio: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val authUid: String = "",
    val followersCount: String = "0",
    val isProfileCreated: Boolean = false,
    val createdAtTimestamp: Long = 0L,
    val coverColorHex: Long = 0xFF3F51B5,
    val coverImageUri: String? = null,
    val avatarColorHex: Long = 0xFF5352ED,
    val avatarImageUri: String? = null,
    val isVerified: Boolean = true,
    val isVip: Boolean = false
) {
    val displayInitial: String
        get() = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "U"

    val displayUserId: String
        get() = when {
            userId.isNotBlank() -> userId
            handle.isNotBlank() -> handle.removePrefix("@")
            else -> "pending_id"
        }

    val isAdmin: Boolean
        get() = email.equals("djsazad@gmail.com", ignoreCase = true) ||
            userId.equals("admin", ignoreCase = true)
}
