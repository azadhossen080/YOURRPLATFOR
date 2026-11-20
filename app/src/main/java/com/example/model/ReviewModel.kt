package com.example.model

enum class ReviewStatus {
    APPROVED,
    PENDING,
    HIDDEN
}

data class ReviewModel(
    val id: String,
    val userId: String,
    val userName: String,
    val rating: Int,
    val comment: String,
    val timestamp: Long,
    val timeAgo: String,
    val status: ReviewStatus = ReviewStatus.APPROVED,
    val isPinned: Boolean = false
)
