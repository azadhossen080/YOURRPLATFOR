package com.example.data

import android.util.Log
import androidx.annotation.Keep
import com.example.model.FriendStatus
import com.example.model.FriendUser
import com.example.model.PostModel
import com.example.model.ReviewModel
import com.example.model.ReviewStatus
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType {
    CREATE,
    UPDATE,
    DELETE,
    LIST,
    GET,
    WRITE
}

fun handleFirestoreError(error: Exception, operationType: OperationType, path: String?) {
    val user = try {
        Firebase.auth.currentUser
    } catch (_: Exception) {
        null
    }
    val payload = JSONObject().apply {
        put("error", error.localizedMessage ?: error.toString())
        put("operationType", operationType.name)
        put("path", path ?: JSONObject.NULL)
        put(
            "authInfo",
            JSONObject().apply {
                put("userId", user?.uid ?: JSONObject.NULL)
                put("email", user?.email ?: JSONObject.NULL)
                put("emailVerified", user?.isEmailVerified ?: false)
                put(
                    "providerInfo",
                    JSONArray().apply {
                        user?.providerData?.forEach { info ->
                            put(
                                JSONObject().apply {
                                    put("providerId", info.providerId)
                                    put("displayName", info.displayName ?: JSONObject.NULL)
                                    put("email", info.email ?: JSONObject.NULL)
                                }
                            )
                        }
                    }
                )
            }
        )
    }
    Log.e("FirestoreError", payload.toString(), error)
}

@Keep
@IgnoreExtraProperties
data class PublicProfileDoc(
    @DocumentId val id: String = "",
    val uid: String = "",
    val name: String = "Guest User",
    val customUserId: String = "",
    val handle: String = "",
    val bio: String = "",
    val followersCount: String = "0",
    val profileCreated: Boolean = false,
    val coverColorHex: Long = 0xFF3F51B5,
    val avatarColorHex: Long = 0xFF5352ED,
    val coverImageUri: String? = null,
    val avatarImageUri: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PrivateInfoDoc(
    @DocumentId val id: String = "",
    val uid: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PostDoc(
    @DocumentId val id: String = "",
    val uid: String = "",
    val userName: String = "",
    val postText: String = "",
    val mediaUri: String? = null,
    val mediaType: String = "text",
    val thumbnailUri: String? = null,
    val video: Boolean = false,
    val likeCount: Int = 0,
    val liked: Boolean = false,
    val comments: List<String> = emptyList(),
    val userPost: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toPostModel(justNowLabel: String): PostModel {
        val elapsedMillis = System.currentTimeMillis() - (createdAt?.toDate()?.time ?: System.currentTimeMillis())
        val timeLabel = when {
            elapsedMillis < 60_000L -> justNowLabel
            elapsedMillis < 3_600_000L -> "${elapsedMillis / 60_000L}m"
            elapsedMillis < 86_400_000L -> "${elapsedMillis / 3_600_000L}h"
            else -> "${elapsedMillis / 86_400_000L}d"
        }
        return PostModel(
            id = id,
            userName = userName,
            timeAgo = timeLabel,
            postText = postText,
            mediaUri = mediaUri,
            mediaType = mediaType,
            thumbnailUri = thumbnailUri,
            isVideo = video,
            likeCount = likeCount,
            isLiked = liked,
            comments = comments,
            isUserPost = userPost
        )
    }
}

@Keep
@IgnoreExtraProperties
data class FriendDoc(
    @DocumentId val id: String = "",
    val uid: String = "",
    val name: String = "",
    val handle: String = "",
    val bio: String = "",
    val mutualFriendsCount: Int = 0,
    val avatarColorHex: Long = 0xFF5352ED,
    val verified: Boolean = false,
    val online: Boolean = false,
    val status: String = FriendStatus.SUGGESTION.name,
    val requestTimeAgo: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toFriendUser(): FriendUser {
        val parsedStatus = enumValues<FriendStatus>().firstOrNull { it.name == status }
            ?: FriendStatus.SUGGESTION
        return FriendUser(
            id = id,
            name = name,
            handle = handle,
            bio = bio,
            mutualFriendsCount = mutualFriendsCount,
            avatarColorHex = avatarColorHex,
            isVerified = verified,
            isOnline = online,
            status = parsedStatus,
            requestTimeAgo = requestTimeAgo
        )
    }
}

@Keep
@IgnoreExtraProperties
data class ReviewDoc(
    @DocumentId val id: String = "",
    val uid: String = "",
    val authorUserId: String = "",
    val userName: String = "",
    val rating: Int = 5,
    val comment: String = "",
    val status: String = ReviewStatus.APPROVED.name,
    val pinned: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toReviewModel(justNowLabel: String): ReviewModel {
        val ts = createdAt?.toDate()?.time ?: System.currentTimeMillis()
        val elapsedMillis = System.currentTimeMillis() - ts
        val timeLabel = when {
            elapsedMillis < 60_000L -> justNowLabel
            elapsedMillis < 3_600_000L -> "${elapsedMillis / 60_000L}m"
            else -> "${elapsedMillis / 3_600_000L}h"
        }
        val parsedStatus = enumValues<ReviewStatus>().firstOrNull { it.name == status }
            ?: ReviewStatus.APPROVED
        return ReviewModel(
            id = id,
            userId = authorUserId,
            userName = userName,
            rating = rating.coerceIn(1, 5),
            comment = comment,
            timestamp = ts,
            timeAgo = timeLabel,
            status = parsedStatus,
            isPinned = pinned
        )
    }
}

object FirestorePayloadMapper {
    fun toPublicProfileCreateMap(uid: String, profile: UserProfile): Map<String, Any> {
        val cleanName = profile.name.trim().ifBlank { "User" }.take(80)
        val cleanCustomId = profile.displayUserId.trim().ifBlank { "user_${uid.take(6)}" }.take(40)
        val cleanHandle = (if (profile.handle.isNotBlank()) profile.handle else "@$cleanCustomId").take(45)
        val raw = mutableMapOf<String, Any?>(
            "uid" to uid,
            "name" to cleanName,
            "customUserId" to cleanCustomId,
            "handle" to cleanHandle,
            "bio" to profile.bio.take(300),
            "followersCount" to profile.followersCount.ifBlank { "0" }.take(20),
            "profileCreated" to profile.isProfileCreated,
            "coverColorHex" to profile.coverColorHex,
            "avatarColorHex" to profile.avatarColorHex,
            "coverImageUri" to profile.coverImageUri?.take(1000),
            "avatarImageUri" to profile.avatarImageUri?.take(1000),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return raw.filterValues { it != null }.mapValues { it.value!! }
    }

    fun toPrivateInfoCreateMap(uid: String, email: String, phoneNumber: String): Map<String, Any> {
        return mapOf(
            "uid" to uid,
            "email" to email.trim().take(120),
            "phoneNumber" to phoneNumber.trim().take(30),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    fun toPostCreateMap(uid: String, post: PostModel): Map<String, Any> {
        val normalizedType = if (post.mediaType == "live") "reels" else post.mediaType
        val allowedType = if (normalizedType in listOf("text", "camera", "gallery", "reels", "gradient")) {
            normalizedType
        } else {
            "text"
        }
        val raw = mutableMapOf<String, Any?>(
            "uid" to uid,
            "userName" to post.userName.trim().ifBlank { "User" }.take(80),
            "postText" to post.postText.trim().ifBlank { "Post" }.take(2000),
            "mediaUri" to post.mediaUri?.take(1000),
            "mediaType" to allowedType,
            "thumbnailUri" to post.thumbnailUri?.take(1000),
            "video" to post.isVideo,
            "likeCount" to post.likeCount.coerceAtLeast(0),
            "liked" to post.isLiked,
            "comments" to post.comments.take(200),
            "userPost" to post.isUserPost,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return raw.filterValues { it != null }.mapValues { it.value!! }
    }

    fun toFriendCreateMap(uid: String, friend: FriendUser): Map<String, Any> {
        return mapOf(
            "uid" to uid,
            "name" to friend.name.trim().ifBlank { "Friend" }.take(80),
            "handle" to friend.handle.trim().ifBlank { "@user" }.take(60),
            "bio" to friend.bio.take(300),
            "mutualFriendsCount" to friend.mutualFriendsCount.coerceAtLeast(0),
            "avatarColorHex" to friend.avatarColorHex,
            "verified" to friend.isVerified,
            "online" to friend.isOnline,
            "status" to friend.status.name,
            "requestTimeAgo" to friend.requestTimeAgo.take(60),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    fun toReviewCreateMap(uid: String, review: ReviewModel): Map<String, Any> {
        return mapOf(
            "uid" to uid,
            "authorUserId" to review.userId.trim().ifBlank { "user" }.take(60),
            "userName" to review.userName.trim().ifBlank { "User" }.take(80),
            "rating" to review.rating.coerceIn(1, 5),
            "comment" to review.comment.trim().ifBlank { "Review" }.take(1000),
            "status" to review.status.name,
            "pinned" to review.isPinned,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}
