package com.example.model

data class PostModel(
    val id: String,
    val userName: String,
    val timeAgo: String,
    val postText: String,
    val mediaUri: String? = null,
    val mediaType: String = "text", // "text", "camera", "gallery", "reels", "gradient"
    val thumbnailUri: String? = null,
    val isVideo: Boolean = false,
    val likeCount: Int = 0,
    val isLiked: Boolean = false,
    val comments: List<String> = emptyList(),
    val isUserPost: Boolean = false
)
