package com.example.data

import android.content.Context
import com.example.R
import com.example.model.FriendStatus
import com.example.model.FriendUser
import com.example.model.PostModel
import com.example.model.ReviewModel
import com.example.model.ReviewStatus
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    companion object {
        fun createOrNull(context: Context): FirestoreRepository? {
            return try {
                val appContext = context.applicationContext
                if (FirebaseApp.getApps(appContext).isEmpty()) {
                    FirebaseApp.initializeApp(appContext)
                }
                val dbId = appContext.getString(R.string.firestore_database_id)
                if (dbId.isBlank()) return null
                FirestoreRepository(FirebaseFirestore.getInstance(dbId))
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun currentAuthUid(): String? {
        return try {
            Firebase.auth.currentUser?.uid
        } catch (_: Exception) {
            null
        }
    }

    private fun requireUserId(): String {
        return currentAuthUid()
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun handleFlowError(error: Throwable, operationType: OperationType, path: String, expectedUid: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) {
            throw error
        }
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && currentAuthUid() == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }

    fun observePublicProfile(uid: String): Flow<PublicProfileDoc?> = flow {
        val path = "users/$uid/public/profile"
        emitAll(
            db.collection("users").document(uid).collection("public").document("profile")
                .snapshots()
                .map { snapshot ->
                    if (!snapshot.exists()) null
                    else snapshot.toObject(
                        PublicProfileDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    fun observePrivateInfo(uid: String): Flow<PrivateInfoDoc?> = flow {
        val path = "users/$uid/private/info"
        emitAll(
            db.collection("users").document(uid).collection("private").document("info")
                .snapshots()
                .map { snapshot ->
                    if (!snapshot.exists()) null
                    else snapshot.toObject(
                        PrivateInfoDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    fun observePosts(uid: String, justNowLabel: String): Flow<List<PostModel>> = flow {
        val path = "users/$uid/posts"
        emitAll(
            db.collection("users").document(uid).collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PostDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).map { it.toPostModel(justNowLabel) }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeFriends(uid: String): Flow<List<FriendUser>> = flow {
        val path = "users/$uid/friends"
        emitAll(
            db.collection("users").document(uid).collection("friends")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FriendDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).map { it.toFriendUser() }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeReviews(uid: String, justNowLabel: String): Flow<List<ReviewModel>> {
        val ownPath = "reviews?uid=$uid"
        val approvedPath = "reviews?status=APPROVED"
        val ownFlow = db.collection("reviews")
            .whereEqualTo("uid", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ReviewDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                handleFlowError(error, OperationType.LIST, ownPath, uid)
                throw error
            }

        val approvedFlow = db.collection("reviews")
            .whereEqualTo("status", ReviewStatus.APPROVED.name)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ReviewDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                handleFlowError(error, OperationType.LIST, approvedPath, uid)
                throw error
            }

        return combine(ownFlow, approvedFlow) { ownList, approvedList ->
            val merged = LinkedHashMap<String, ReviewModel>()
            ownList.forEach { doc -> merged[doc.id] = doc.toReviewModel(justNowLabel) }
            approvedList.forEach { doc ->
                if (!merged.containsKey(doc.id)) {
                    merged[doc.id] = doc.toReviewModel(justNowLabel)
                }
            }
            merged.values.sortedWith(
                compareByDescending<ReviewModel> { it.isPinned }.thenByDescending { it.timestamp }
            )
        }
    }

    suspend fun upsertPublicProfile(profile: UserProfile): Result<Unit> {
        var docPath = "users/${currentAuthUid() ?: "unauthenticated"}/public/profile"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("public").document("profile")
            docPath = docRef.path
            val existing = docRef.get().await()
            if (!existing.exists()) {
                docRef.set(FirestorePayloadMapper.toPublicProfileCreateMap(uid, profile)).await()
            } else {
                val cleanName = profile.name.trim().ifBlank { "User" }.take(80)
                val cleanCustomId = profile.displayUserId.trim().ifBlank { "user_${uid.take(6)}" }.take(40)
                val cleanHandle = (if (profile.handle.isNotBlank()) profile.handle else "@$cleanCustomId").take(45)
                val updates = mutableMapOf<String, Any>(
                    "name" to cleanName,
                    "customUserId" to cleanCustomId,
                    "handle" to cleanHandle,
                    "bio" to profile.bio.take(300),
                    "followersCount" to profile.followersCount.ifBlank { "0" }.take(20),
                    "profileCreated" to profile.isProfileCreated,
                    "coverColorHex" to profile.coverColorHex,
                    "avatarColorHex" to profile.avatarColorHex,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                updates["coverImageUri"] = profile.coverImageUri?.take(1000) ?: FieldValue.delete()
                updates["avatarImageUri"] = profile.avatarImageUri?.take(1000) ?: FieldValue.delete()
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun upsertPrivateInfo(email: String, phoneNumber: String): Result<Unit> {
        var docPath = "users/${currentAuthUid() ?: "unauthenticated"}/private/info"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("private").document("info")
            docPath = docRef.path
            val existing = docRef.get().await()
            if (!existing.exists()) {
                docRef.set(FirestorePayloadMapper.toPrivateInfoCreateMap(uid, email, phoneNumber)).await()
            } else {
                docRef.update(
                    mapOf(
                        "email" to email.trim().take(120),
                        "phoneNumber" to phoneNumber.trim().take(30),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun savePost(post: PostModel): Result<Unit> {
        var docPath = "users/${currentAuthUid() ?: "unauthenticated"}/posts/${post.id}"
        return try {
            val uid = requireUserId()
            require(post.id.isNotBlank()) { "Post ID must not be blank" }
            val docRef = db.collection("users").document(uid).collection("posts").document(post.id)
            docPath = docRef.path
            docRef.set(FirestorePayloadMapper.toPostCreateMap(uid, post)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updatePostEngagement(
        postId: String,
        likeCount: Int,
        isLiked: Boolean,
        comments: List<String>
    ): Result<Unit> {
        var docPath = "users/${currentAuthUid() ?: "unauthenticated"}/posts/$postId"
        return try {
            val uid = requireUserId()
            require(postId.isNotBlank()) { "Post ID must not be blank" }
            val docRef = db.collection("users").document(uid).collection("posts").document(postId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "likeCount" to likeCount.coerceAtLeast(0),
                    "liked" to isLiked,
                    "comments" to comments.take(200),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> {
        var docPath = "users/${currentAuthUid() ?: "unauthenticated"}/posts/$postId"
        return try {
            val uid = requireUserId()
            require(postId.isNotBlank()) { "Post ID must not be blank" }
            val docRef = db.collection("users").document(uid).collection("posts").document(postId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveFriend(friend: FriendUser): Result<Unit> {
        var docPath = "users/${currentAuthUid() ?: "unauthenticated"}/friends/${friend.id}"
        return try {
            val uid = requireUserId()
            require(friend.id.isNotBlank()) { "Friend ID must not be blank" }
            val docRef = db.collection("users").document(uid).collection("friends").document(friend.id)
            docPath = docRef.path
            val existing = docRef.get().await()
            if (!existing.exists()) {
                docRef.set(FirestorePayloadMapper.toFriendCreateMap(uid, friend)).await()
            } else {
                docRef.update(
                    mapOf(
                        "status" to friend.status.name,
                        "requestTimeAgo" to friend.requestTimeAgo.take(60),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateFriendStatus(
        friendId: String,
        status: FriendStatus,
        requestTimeAgo: String
    ): Result<Unit> {
        var docPath = "users/${currentAuthUid() ?: "unauthenticated"}/friends/$friendId"
        return try {
            val uid = requireUserId()
            require(friendId.isNotBlank()) { "Friend ID must not be blank" }
            val docRef = db.collection("users").document(uid).collection("friends").document(friendId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "status" to status.name,
                    "requestTimeAgo" to requestTimeAgo.take(60),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteFriend(friendId: String): Result<Unit> {
        var docPath = "users/${currentAuthUid() ?: "unauthenticated"}/friends/$friendId"
        return try {
            val uid = requireUserId()
            require(friendId.isNotBlank()) { "Friend ID must not be blank" }
            val docRef = db.collection("users").document(uid).collection("friends").document(friendId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addReview(review: ReviewModel): Result<Unit> {
        var docPath = "reviews/${review.id}"
        return try {
            val uid = requireUserId()
            require(review.id.isNotBlank()) { "Review ID must not be blank" }
            val docRef = db.collection("reviews").document(review.id)
            docPath = docRef.path
            docRef.set(FirestorePayloadMapper.toReviewCreateMap(uid, review)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateReviewControl(
        reviewId: String,
        status: ReviewStatus,
        isPinned: Boolean
    ): Result<Unit> {
        val docPath = "reviews/$reviewId"
        return try {
            requireUserId()
            require(reviewId.isNotBlank()) { "Review ID must not be blank" }
            db.collection("reviews").document(reviewId).update(
                mapOf(
                    "status" to status.name,
                    "pinned" to isPinned,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteReview(reviewId: String): Result<Unit> {
        val docPath = "reviews/$reviewId"
        return try {
            requireUserId()
            require(reviewId.isNotBlank()) { "Review ID must not be blank" }
            db.collection("reviews").document(reviewId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun seedDefaultFriendsIfEmpty(defaults: List<FriendUser>): Result<Unit> {
        var path = "users/${currentAuthUid() ?: "unauthenticated"}/friends"
        return try {
            val uid = requireUserId()
            val col = db.collection("users").document(uid).collection("friends")
            path = col.path
            val snapshot = col.limit(1).get().await()
            if (snapshot.isEmpty && defaults.isNotEmpty()) {
                val batch = db.batch()
                defaults.forEach { friend ->
                    batch.set(col.document(friend.id), FirestorePayloadMapper.toFriendCreateMap(uid, friend))
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun seedDefaultPostIfEmpty(defaultPost: PostModel): Result<Unit> {
        var path = "users/${currentAuthUid() ?: "unauthenticated"}/posts"
        return try {
            val uid = requireUserId()
            val col = db.collection("users").document(uid).collection("posts")
            path = col.path
            val snapshot = col.limit(1).get().await()
            if (snapshot.isEmpty) {
                col.document(defaultPost.id)
                    .set(FirestorePayloadMapper.toPostCreateMap(uid, defaultPost))
                    .await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }
}
