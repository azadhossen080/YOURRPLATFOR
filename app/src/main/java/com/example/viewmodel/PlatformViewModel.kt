package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiRepository
import com.example.data.FirestoreRepository
import com.example.data.LocalRepository
import com.example.model.AppSettings
import com.example.model.FriendStatus
import com.example.model.FriendUser
import com.example.model.NotificationModel
import com.example.model.PostModel
import com.example.model.ReviewModel
import com.example.model.ReviewStatus
import com.example.model.UserProfile
import com.example.util.AppStringsRegistry
import com.example.util.MediaProcessor
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

enum class UploadStage {
    PREPARING,
    PROCESSING_MEDIA,
    UPLOADING,
    FINALIZING
}

sealed interface PostUploadState {
    data object Idle : PostUploadState
    data class Uploading(
        val postText: String,
        val mediaType: String,
        val previewUri: String?,
        val progress: Float,
        val stage: UploadStage
    ) : PostUploadState

    data class Success(val postId: String) : PostUploadState
    data class Failed(val message: String) : PostUploadState
}

sealed interface FirestoreSyncState {
    data object Idle : FirestoreSyncState
    data object Syncing : FirestoreSyncState
    data object Synced : FirestoreSyncState
    data class Error(val message: String) : FirestoreSyncState
}

private data class PendingUploadRequest(
    val text: String,
    val type: String,
    val mediaUri: String?,
    val previewUri: String?
)

class PlatformViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LocalRepository(application.applicationContext)
    private val firestoreRepo: FirestoreRepository? = FirestoreRepository.createOrNull(application.applicationContext)
    val aiRepository = AiRepository()

    private val _posts = MutableStateFlow<List<PostModel>>(emptyList())
    val posts: StateFlow<List<PostModel>> = _posts.asStateFlow()

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _notifications = MutableStateFlow<List<NotificationModel>>(emptyList())
    val notifications: StateFlow<List<NotificationModel>> = _notifications.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _friends = MutableStateFlow<List<FriendUser>>(emptyList())
    val friends: StateFlow<List<FriendUser>> = _friends.asStateFlow()

    private val _reviews = MutableStateFlow<List<ReviewModel>>(emptyList())
    val reviews: StateFlow<List<ReviewModel>> = _reviews.asStateFlow()

    private val _lastExportPath = MutableStateFlow<String?>(null)
    val lastExportPath: StateFlow<String?> = _lastExportPath.asStateFlow()

    private val _lastExportTimestamp = MutableStateFlow<Long?>(null)
    val lastExportTimestamp: StateFlow<Long?> = _lastExportTimestamp.asStateFlow()

    private val _uploadState = MutableStateFlow<PostUploadState>(PostUploadState.Idle)
    val uploadState: StateFlow<PostUploadState> = _uploadState.asStateFlow()

    private val _cloudSyncState = MutableStateFlow<FirestoreSyncState>(FirestoreSyncState.Idle)
    val cloudSyncState: StateFlow<FirestoreSyncState> = _cloudSyncState.asStateFlow()

    private var lastUploadRequest: PendingUploadRequest? = null
    private var activeUid: String? = null
    private val firestoreListenerJobs = mutableListOf<Job>()
    private var seededInitialDefaultsForUid: String? = null

    init {
        _posts.value = repository.loadPosts()
        _userProfile.value = repository.loadProfile()
        _settings.value = repository.loadSettings()
        _notifications.value = repository.loadNotifications()
        _friends.value = repository.loadFriends()
        _reviews.value = repository.loadReviews()
        _lastExportPath.value = repository.getLastExportPath()
        _lastExportTimestamp.value = repository.getLastExportTime()
    }

    private fun currentStrings() = AppStringsRegistry.forCode(_settings.value.selectedLanguageCode)

    private fun signedInUid(): String? {
        return try {
            Firebase.auth.currentUser?.uid
        } catch (_: Exception) {
            null
        }
    }

    fun bindAuthenticatedUser(
        uid: String,
        googleDisplayName: String?,
        googleEmail: String?
    ) {
        if (activeUid == uid && firestoreListenerJobs.isNotEmpty()) return
        cancelFirestoreListeners()
        activeUid = uid

        val repo = firestoreRepo ?: return
        _cloudSyncState.value = FirestoreSyncState.Syncing

        val localCached = _userProfile.value
        if (localCached.authUid != uid) {
            _userProfile.value = UserProfile(
                name = googleDisplayName?.takeIf { it.isNotBlank() } ?: "Guest User",
                email = googleEmail ?: "",
                authUid = uid,
                followersCount = "0",
                isProfileCreated = false
            )
        }

        // 1. Observe public profile + private PII doc
        firestoreListenerJobs += viewModelScope.launch {
            combine(
                repo.observePublicProfile(uid),
                repo.observePrivateInfo(uid)
            ) { pubDoc, privDoc ->
                Pair(pubDoc, privDoc)
            }.catch { error ->
                if (error !is CancellationException && signedInUid() == uid) {
                    _cloudSyncState.value = FirestoreSyncState.Error(error.localizedMessage ?: "Sync error")
                }
            }.collect { (pubDoc, privDoc) ->
                if (pubDoc != null) {
                    val mapped = UserProfile(
                        name = pubDoc.name,
                        userId = pubDoc.customUserId,
                        handle = pubDoc.handle,
                        bio = pubDoc.bio,
                        email = privDoc?.email ?: googleEmail.orEmpty(),
                        phoneNumber = privDoc?.phoneNumber ?: "",
                        authUid = uid,
                        followersCount = pubDoc.followersCount.ifBlank { "0" },
                        isProfileCreated = pubDoc.profileCreated,
                        createdAtTimestamp = pubDoc.createdAt?.toDate()?.time ?: System.currentTimeMillis(),
                        coverColorHex = pubDoc.coverColorHex,
                        coverImageUri = pubDoc.coverImageUri,
                        avatarColorHex = pubDoc.avatarColorHex,
                        avatarImageUri = pubDoc.avatarImageUri
                    )
                    _userProfile.value = mapped
                    repository.saveProfile(mapped)
                    _cloudSyncState.value = FirestoreSyncState.Synced
                } else {
                    val pending = _userProfile.value.copy(
                        name = _userProfile.value.name.takeIf { it != "Guest User" }
                            ?: googleDisplayName.orEmpty().ifBlank { "Guest User" },
                        email = privDoc?.email?.ifBlank { googleEmail.orEmpty() } ?: googleEmail.orEmpty(),
                        phoneNumber = privDoc?.phoneNumber ?: _userProfile.value.phoneNumber,
                        authUid = uid,
                        isProfileCreated = false
                    )
                    _userProfile.value = pending
                    _cloudSyncState.value = FirestoreSyncState.Synced
                }
            }
        }

        // 2. Observe user posts
        firestoreListenerJobs += viewModelScope.launch {
            repo.observePosts(uid, currentStrings().justNow)
                .catch { error ->
                    if (error !is CancellationException && signedInUid() == uid) {
                        _cloudSyncState.value = FirestoreSyncState.Error(error.localizedMessage ?: "Posts sync error")
                    }
                }
                .collect { remotePosts ->
                    if (remotePosts.isEmpty() && seededInitialDefaultsForUid != uid) {
                        seededInitialDefaultsForUid = uid
                        val seedPost = repository.loadPosts().firstOrNull { !it.isUserPost }
                        if (seedPost != null) {
                            repo.seedDefaultPostIfEmpty(seedPost)
                        }
                    } else if (remotePosts.isNotEmpty()) {
                        _posts.value = remotePosts
                        repository.savePosts(remotePosts)
                    }
                }
        }

        // 3. Observe user friends
        firestoreListenerJobs += viewModelScope.launch {
            repo.observeFriends(uid)
                .catch { error ->
                    if (error !is CancellationException && signedInUid() == uid) {
                        _cloudSyncState.value = FirestoreSyncState.Error(error.localizedMessage ?: "Friends sync error")
                    }
                }
                .collect { remoteFriends ->
                    if (remoteFriends.isEmpty()) {
                        repo.seedDefaultFriendsIfEmpty(repository.loadFriends())
                    } else {
                        _friends.value = remoteFriends
                        repository.saveFriends(remoteFriends)
                    }
                }
        }

        // 4. Observe reviews
        firestoreListenerJobs += viewModelScope.launch {
            repo.observeReviews(uid, currentStrings().justNow)
                .catch { error ->
                    if (error !is CancellationException && signedInUid() == uid) {
                        _cloudSyncState.value = FirestoreSyncState.Error(error.localizedMessage ?: "Reviews sync error")
                    }
                }
                .collect { remoteReviews ->
                    _reviews.value = remoteReviews
                    repository.saveReviews(remoteReviews)
                }
        }
    }

    fun prepareForSignOut() {
        cancelFirestoreListeners()
        activeUid = null
        seededInitialDefaultsForUid = null
        _cloudSyncState.value = FirestoreSyncState.Idle
        _selectedTab.value = 0
    }

    private fun cancelFirestoreListeners() {
        firestoreListenerJobs.forEach { it.cancel() }
        firestoreListenerJobs.clear()
    }

    override fun onCleared() {
        cancelFirestoreListeners()
        super.onCleared()
    }

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun submitPost(
        text: String,
        type: String,
        mediaUri: String?,
        previewUri: String? = mediaUri,
        onPublished: () -> Unit = {}
    ) {
        if (_uploadState.value is PostUploadState.Uploading) return

        val finalCaption = if (text.isBlank()) currentStrings().defaultMediaPostText else text.trim()
        lastUploadRequest = PendingUploadRequest(finalCaption, type, mediaUri, previewUri)
        _uploadState.value = PostUploadState.Uploading(
            postText = finalCaption,
            mediaType = type,
            previewUri = previewUri ?: mediaUri,
            progress = 0.08f,
            stage = UploadStage.PREPARING
        )

        viewModelScope.launch {
            try {
                delay(200)

                var finalMediaUri = mediaUri
                var finalThumbnailUri = previewUri
                var isVideo = type == "reels"

                if (!mediaUri.isNullOrBlank()) {
                    _uploadState.value = PostUploadState.Uploading(
                        postText = finalCaption,
                        mediaType = type,
                        previewUri = previewUri ?: mediaUri,
                        progress = 0.22f,
                        stage = UploadStage.PROCESSING_MEDIA
                    )

                    val processed = MediaProcessor.processAndSaveMedia(
                        context = getApplication(),
                        sourceUriString = mediaUri,
                        requestedType = type
                    ) { mediaFrac ->
                        val mapped = 0.22f + mediaFrac * 0.43f
                        _uploadState.value = PostUploadState.Uploading(
                            postText = finalCaption,
                            mediaType = type,
                            previewUri = previewUri ?: mediaUri,
                            progress = mapped.coerceIn(0.22f, 0.65f),
                            stage = UploadStage.PROCESSING_MEDIA
                        )
                    }
                    finalMediaUri = processed.mediaUri
                    finalThumbnailUri = processed.thumbnailUri ?: previewUri
                    isVideo = processed.isVideo
                } else {
                    _uploadState.value = PostUploadState.Uploading(
                        postText = finalCaption,
                        mediaType = type,
                        previewUri = null,
                        progress = 0.45f,
                        stage = UploadStage.PROCESSING_MEDIA
                    )
                    delay(200)
                }

                for (step in listOf(0.72f, 0.85f, 0.94f)) {
                    _uploadState.value = PostUploadState.Uploading(
                        postText = finalCaption,
                        mediaType = type,
                        previewUri = finalThumbnailUri ?: finalMediaUri,
                        progress = step,
                        stage = UploadStage.UPLOADING
                    )
                    delay(220)
                }

                _uploadState.value = PostUploadState.Uploading(
                    postText = finalCaption,
                    mediaType = type,
                    previewUri = finalThumbnailUri ?: finalMediaUri,
                    progress = 0.99f,
                    stage = UploadStage.FINALIZING
                )
                delay(160)

                val newPost = PostModel(
                    id = UUID.randomUUID().toString(),
                    userName = _userProfile.value.name,
                    timeAgo = currentStrings().justNow,
                    postText = finalCaption,
                    mediaUri = finalMediaUri,
                    mediaType = type,
                    thumbnailUri = finalThumbnailUri,
                    isVideo = isVideo,
                    likeCount = 0,
                    isLiked = false,
                    comments = emptyList(),
                    isUserPost = true
                )
                val updated = listOf(newPost) + _posts.value
                _posts.value = updated
                repository.savePosts(updated)

                if (signedInUid() != null) {
                    firestoreRepo?.savePost(newPost)
                }

                _uploadState.value = PostUploadState.Success(newPost.id)
                _selectedTab.value = 0
                onPublished()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uploadState.value = PostUploadState.Failed(
                    e.localizedMessage ?: currentStrings().uploadFailedBanner
                )
            }
        }
    }

    fun retryLastUpload(onPublished: () -> Unit = {}) {
        val req = lastUploadRequest ?: return
        submitPost(
            text = req.text,
            type = req.type,
            mediaUri = req.mediaUri,
            previewUri = req.previewUri,
            onPublished = onPublished
        )
    }

    fun dismissUploadState() {
        if (_uploadState.value !is PostUploadState.Uploading) {
            _uploadState.value = PostUploadState.Idle
        }
    }

    fun addNewPost(text: String, type: String, mediaUri: String?) {
        val newPost = PostModel(
            id = UUID.randomUUID().toString(),
            userName = _userProfile.value.name,
            timeAgo = currentStrings().justNow,
            postText = text,
            mediaUri = mediaUri,
            mediaType = type,
            isVideo = type == "reels",
            likeCount = 0,
            isLiked = false,
            comments = emptyList(),
            isUserPost = true
        )
        val updated = listOf(newPost) + _posts.value
        _posts.value = updated
        repository.savePosts(updated)
        _selectedTab.value = 0
        if (signedInUid() != null) {
            viewModelScope.launch { firestoreRepo?.savePost(newPost) }
        }
    }

    fun deletePost(id: String) {
        val updated = _posts.value.filter { it.id != id }
        _posts.value = updated
        repository.savePosts(updated)
        if (signedInUid() != null) {
            viewModelScope.launch { firestoreRepo?.deletePost(id) }
        }
    }

    fun toggleLike(id: String) {
        var targetPost: PostModel? = null
        val updated = _posts.value.map { post ->
            if (post.id == id) {
                val newLiked = !post.isLiked
                val newCount = if (newLiked) post.likeCount + 1 else (post.likeCount - 1).coerceAtLeast(0)
                post.copy(isLiked = newLiked, likeCount = newCount).also { targetPost = it }
            } else post
        }
        _posts.value = updated
        repository.savePosts(updated)
        val synced = targetPost
        if (synced != null && signedInUid() != null) {
            viewModelScope.launch {
                firestoreRepo?.updatePostEngagement(
                    postId = synced.id,
                    likeCount = synced.likeCount,
                    isLiked = synced.isLiked,
                    comments = synced.comments
                )
            }
        }
    }

    fun addComment(id: String, comment: String) {
        if (comment.isBlank()) return
        var targetPost: PostModel? = null
        val updated = _posts.value.map { post ->
            if (post.id == id) {
                post.copy(comments = post.comments + comment.trim()).also { targetPost = it }
            } else post
        }
        _posts.value = updated
        repository.savePosts(updated)
        val synced = targetPost
        if (synced != null && signedInUid() != null) {
            viewModelScope.launch {
                firestoreRepo?.updatePostEngagement(
                    postId = synced.id,
                    likeCount = synced.likeCount,
                    isLiked = synced.isLiked,
                    comments = synced.comments
                )
            }
        }
    }

    fun updateCoverColor(colorHex: Long) {
        val updated = _userProfile.value.copy(coverColorHex = colorHex, coverImageUri = null)
        _userProfile.value = updated
        repository.saveProfile(updated)
        syncProfileToFirestore(updated)
    }

    fun updateCoverUri(uri: String?) {
        if (uri == null) {
            val updated = _userProfile.value.copy(coverImageUri = null)
            _userProfile.value = updated
            repository.saveProfile(updated)
            syncProfileToFirestore(updated)
            return
        }
        viewModelScope.launch {
            val savedUri = MediaProcessor.persistImageToFilesDir(getApplication(), uri, "cover")
            val updated = _userProfile.value.copy(coverImageUri = savedUri)
            _userProfile.value = updated
            repository.saveProfile(updated)
            syncProfileToFirestore(updated)
        }
    }

    fun updateAvatarColor(colorHex: Long) {
        val updated = _userProfile.value.copy(avatarColorHex = colorHex, avatarImageUri = null)
        _userProfile.value = updated
        repository.saveProfile(updated)
        syncProfileToFirestore(updated)
    }

    fun updateAvatarUri(uri: String?) {
        if (uri == null) {
            val updated = _userProfile.value.copy(avatarImageUri = null)
            _userProfile.value = updated
            repository.saveProfile(updated)
            syncProfileToFirestore(updated)
            return
        }
        viewModelScope.launch {
            val savedUri = MediaProcessor.persistImageToFilesDir(getApplication(), uri, "avatar")
            val updated = _userProfile.value.copy(avatarImageUri = savedUri)
            _userProfile.value = updated
            repository.saveProfile(updated)
            syncProfileToFirestore(updated)
        }
    }

    fun updateBio(bio: String) {
        val updated = _userProfile.value.copy(bio = bio)
        _userProfile.value = updated
        repository.saveProfile(updated)
        syncProfileToFirestore(updated)
    }

    fun updateSettings(newSettings: AppSettings) {
        _settings.value = newSettings
        repository.saveSettings(newSettings)
    }

    fun markNotificationsRead() {
        val updated = _notifications.value.map { it.copy(isRead = true) }
        _notifications.value = updated
        repository.saveNotifications(updated)
    }

    fun acceptFriendRequest(userId: String) {
        var acceptedName = ""
        val updated = _friends.value.map { user ->
            if (user.id == userId) {
                acceptedName = user.name
                user.copy(status = FriendStatus.FRIEND, requestTimeAgo = "")
            } else user
        }
        _friends.value = updated
        repository.saveFriends(updated)
        if (signedInUid() != null) {
            viewModelScope.launch {
                firestoreRepo?.updateFriendStatus(userId, FriendStatus.FRIEND, "")
            }
        }

        if (acceptedName.isNotEmpty()) {
            val strings = currentStrings()
            val newNotif = NotificationModel(
                id = UUID.randomUUID().toString(),
                title = strings.nowFriendsNotification(acceptedName),
                timeAgo = strings.justNow,
                iconType = "friend",
                isRead = false
            )
            val updatedNotifs = listOf(newNotif) + _notifications.value
            _notifications.value = updatedNotifs
            repository.saveNotifications(updatedNotifs)
        }
    }

    fun declineFriendRequest(userId: String) {
        val updated = _friends.value.map { user ->
            if (user.id == userId) {
                user.copy(status = FriendStatus.SUGGESTION, requestTimeAgo = "")
            } else user
        }
        _friends.value = updated
        repository.saveFriends(updated)
        if (signedInUid() != null) {
            viewModelScope.launch {
                firestoreRepo?.updateFriendStatus(userId, FriendStatus.SUGGESTION, "")
            }
        }
    }

    fun sendFriendRequest(userId: String) {
        val now = currentStrings().justNow
        val updated = _friends.value.map { user ->
            if (user.id == userId) {
                user.copy(status = FriendStatus.OUTGOING_REQUEST, requestTimeAgo = now)
            } else user
        }
        _friends.value = updated
        repository.saveFriends(updated)
        if (signedInUid() != null) {
            viewModelScope.launch {
                firestoreRepo?.updateFriendStatus(userId, FriendStatus.OUTGOING_REQUEST, now)
            }
        }
    }

    fun cancelFriendRequest(userId: String) {
        val updated = _friends.value.map { user ->
            if (user.id == userId) {
                user.copy(status = FriendStatus.SUGGESTION, requestTimeAgo = "")
            } else user
        }
        _friends.value = updated
        repository.saveFriends(updated)
        if (signedInUid() != null) {
            viewModelScope.launch {
                firestoreRepo?.updateFriendStatus(userId, FriendStatus.SUGGESTION, "")
            }
        }
    }

    fun unfriend(userId: String) {
        val updated = _friends.value.map { user ->
            if (user.id == userId) {
                user.copy(status = FriendStatus.SUGGESTION, requestTimeAgo = "")
            } else user
        }
        _friends.value = updated
        repository.saveFriends(updated)
        if (signedInUid() != null) {
            viewModelScope.launch {
                firestoreRepo?.updateFriendStatus(userId, FriendStatus.SUGGESTION, "")
            }
        }
    }

    fun removeSuggestion(userId: String) {
        val updated = _friends.value.filter { it.id != userId }
        _friends.value = updated
        repository.saveFriends(updated)
        if (signedInUid() != null) {
            viewModelScope.launch {
                firestoreRepo?.deleteFriend(userId)
            }
        }
    }

    fun createOrUpdateIdentity(
        userName: String,
        userId: String,
        email: String = _userProfile.value.email,
        phoneNumber: String = _userProfile.value.phoneNumber
    ) {
        val cleanName = userName.trim().ifBlank { "Guest User" }
        val cleanId = userId.trim().removePrefix("@").replace("\\s+".toRegex(), "_")
        val current = _userProfile.value
        val uid = signedInUid() ?: current.authUid
        val updated = current.copy(
            name = cleanName,
            userId = cleanId,
            handle = if (cleanId.isNotBlank()) "@$cleanId" else "",
            email = email.trim(),
            phoneNumber = phoneNumber.trim(),
            authUid = uid,
            followersCount = current.followersCount.ifBlank { "0" },
            isProfileCreated = true,
            createdAtTimestamp = if (current.createdAtTimestamp > 0L) current.createdAtTimestamp else System.currentTimeMillis()
        )
        _userProfile.value = updated
        repository.saveProfile(updated)
        if (signedInUid() != null) {
            viewModelScope.launch {
                firestoreRepo?.upsertPublicProfile(updated)
                firestoreRepo?.upsertPrivateInfo(updated.email, updated.phoneNumber)
            }
        }
    }

    private fun syncProfileToFirestore(profile: UserProfile) {
        if (signedInUid() != null) {
            viewModelScope.launch {
                firestoreRepo?.upsertPublicProfile(profile)
            }
        }
    }

    fun submitReview(rating: Int, comment: String) {
        if (comment.isBlank()) return
        val profile = _userProfile.value
        val newReview = ReviewModel(
            id = UUID.randomUUID().toString(),
            userId = profile.displayUserId,
            userName = profile.name,
            rating = rating.coerceIn(1, 5),
            comment = comment.trim(),
            timestamp = System.currentTimeMillis(),
            timeAgo = currentStrings().justNow,
            status = ReviewStatus.APPROVED,
            isPinned = false
        )
        val updated = listOf(newReview) + _reviews.value
        _reviews.value = updated
        repository.saveReviews(updated)
        if (signedInUid() != null) {
            viewModelScope.launch { firestoreRepo?.addReview(newReview) }
        }
    }

    fun updateReviewStatus(reviewId: String, status: ReviewStatus) {
        var target: ReviewModel? = null
        val updated = _reviews.value.map { r ->
            if (r.id == reviewId) r.copy(status = status).also { target = it } else r
        }
        _reviews.value = updated
        repository.saveReviews(updated)
        val synced = target
        if (synced != null && signedInUid() != null) {
            viewModelScope.launch {
                firestoreRepo?.updateReviewControl(synced.id, synced.status, synced.isPinned)
            }
        }
    }

    fun togglePinReview(reviewId: String) {
        var target: ReviewModel? = null
        val updated = _reviews.value.map { r ->
            if (r.id == reviewId) r.copy(isPinned = !r.isPinned).also { target = it } else r
        }
        _reviews.value = updated
        repository.saveReviews(updated)
        val synced = target
        if (synced != null && signedInUid() != null) {
            viewModelScope.launch {
                firestoreRepo?.updateReviewControl(synced.id, synced.status, synced.isPinned)
            }
        }
    }

    fun deleteReview(reviewId: String) {
        val updated = _reviews.value.filter { it.id != reviewId }
        _reviews.value = updated
        repository.saveReviews(updated)
        if (signedInUid() != null) {
            viewModelScope.launch { firestoreRepo?.deleteReview(reviewId) }
        }
    }

    fun buildCollectedUserDataJson(): String {
        return repository.buildCollectedUserDataJson(
            profile = _userProfile.value,
            posts = _posts.value,
            friends = _friends.value,
            reviews = _reviews.value,
            settings = _settings.value
        )
    }

    fun exportCollectedUserData(): File? {
        val json = buildCollectedUserDataJson()
        val file = repository.exportCollectedUserDataToFile(json, _userProfile.value.displayUserId)
        if (file != null) {
            _lastExportPath.value = file.absolutePath
            _lastExportTimestamp.value = System.currentTimeMillis()
        }
        return file
    }
}
