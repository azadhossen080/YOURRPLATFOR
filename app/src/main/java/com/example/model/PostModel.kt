package com.example.model

data class PostModel(
    val id: String,
    val userName: String,
    val timeAgo: String,
    val postText: String,
    val mediaUri: String? = null,
    val mediaType: String = "text", // "text", "camera", "gallery", "reels", "live", "gradient"
    val thumbnailUri: String? = null,
    val isVideo: Boolean = false,
    val likeCount: Int = 0,
    val isLiked: Boolean = false,
    val comments: List<String> = emptyList(),
    val isUserPost: Boolean = false,
    val colorFilter: String = ColorFilterMode.NONE.id,
    val transitionEffect: String = TransitionEffect.NONE.id,
    val mojiStickers: List<String> = emptyList(),
    val playbackSpeed: Float = 1.0f,
    val isMuted: Boolean = false,
    val trimStartFraction: Float = 0f,
    val trimEndFraction: Float = 1f,
    val isLiveStream: Boolean = false
)

