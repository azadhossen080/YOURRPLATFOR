package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.LocalAccount
import com.example.data.LocalAccountStore
import com.example.data.LocalRepository
import com.example.model.FriendStatus
import com.example.model.PostModel
import com.example.ui.components.ActivityCenterSheet
import com.example.ui.components.AppHeader
import com.example.ui.components.BottomNavBar
import com.example.ui.components.CommentBottomSheet
import com.example.ui.components.CreateIdentityDialog
import com.example.ui.components.QrCodeDialog
import com.example.ui.screens.AiStudioScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CompleteRegistrationScreen
import com.example.ui.screens.CreatePostScreen
import com.example.ui.screens.FriendsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ReviewsAndStorageScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.WavesScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.AppStringsRegistry
import com.example.util.GoogleAuthHelper
import com.example.util.LocalAppStrings
import com.example.util.configureAppCheck
import com.example.viewmodel.PlatformViewModel
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import java.util.Locale

enum class SubScreen {
    NOTIFICATIONS,
    SETTINGS,
    AI_STUDIO,
    FRIENDS,
    REVIEWS_STORAGE
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configure App Check synchronously before Firebase calls or ViewModels
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            var showSplash by remember { mutableStateOf(true) }
            val firebaseReady = remember { FirebaseApp.getApps(context).isNotEmpty() }
            var currentUser by remember {
                mutableStateOf<FirebaseUser?>(
                    if (firebaseReady) try { Firebase.auth.currentUser } catch (_: Exception) { null } else null
                )
            }

            if (firebaseReady) {
                DisposableEffect(Unit) {
                    val auth = Firebase.auth
                    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                        currentUser = firebaseAuth.currentUser
                    }
                    auth.addAuthStateListener(listener)
                    onDispose {
                        auth.removeAuthStateListener(listener)
                    }
                }
            }

            if (showSplash) {
                val initialSettings = remember { LocalRepository(context).loadSettings() }
                val splashStrings = remember(initialSettings.selectedLanguageCode) {
                    AppStringsRegistry.forCode(initialSettings.selectedLanguageCode)
                }
                CompositionLocalProvider(LocalAppStrings provides splashStrings) {
                    MyApplicationTheme(darkTheme = initialSettings.isDarkMode) {
                        SplashScreen(onSplashFinished = { showSplash = false })
                    }
                }
                return@setContent
            }

            val signedInUser = currentUser
            val accountStore = remember(context) { LocalAccountStore(context) }
            var activeLocalAccount by remember {
                mutableStateOf<LocalAccount?>(accountStore.getActiveAccount())
            }
            var bypassAuth by remember { mutableStateOf(false) }

            if (signedInUser == null && activeLocalAccount == null && !bypassAuth) {
                val preAuthSettings = remember { LocalRepository(context).loadSettings() }
                val preAuthStrings = remember(preAuthSettings.selectedLanguageCode) {
                    AppStringsRegistry.forCode(preAuthSettings.selectedLanguageCode)
                }
                val preAuthLayoutDir = remember(preAuthSettings.selectedLanguageCode) {
                    if (AppStringsRegistry.isRtl(preAuthSettings.selectedLanguageCode)) {
                        LayoutDirection.Rtl
                    } else {
                        LayoutDirection.Ltr
                    }
                }
                CompositionLocalProvider(
                    LocalAppStrings provides preAuthStrings,
                    LocalLayoutDirection provides preAuthLayoutDir
                ) {
                    MyApplicationTheme(darkTheme = preAuthSettings.isDarkMode) {
                        AuthScreen(
                            onAuthSuccess = {
                                bypassAuth = false
                                currentUser = try {
                                    Firebase.auth.currentUser
                                } catch (_: Exception) {
                                    null
                                }
                            },
                            onLocalAuthSuccess = { account ->
                                bypassAuth = false
                                activeLocalAccount = account
                            },
                            onCancelAndUsePlatform = {
                                bypassAuth = true
                            }
                        )
                    }
                }
            } else {
                val uid = signedInUser?.uid ?: activeLocalAccount?.id ?: "guest"
                key(uid) {
                    val viewModel: PlatformViewModel = viewModel(key = uid)
                    LaunchedEffect(signedInUser?.uid, activeLocalAccount?.id) {
                        if (signedInUser != null) {
                            viewModel.bindAuthenticatedUser(
                                uid = signedInUser.uid,
                                googleDisplayName = signedInUser.displayName,
                                googleEmail = signedInUser.email
                            )
                        } else if (activeLocalAccount != null) {
                            viewModel.bindLocalAccount(activeLocalAccount!!)
                        }
                    }

                    val settings by viewModel.settings.collectAsState()
                    val localizedStrings = remember(settings.selectedLanguageCode) {
                        AppStringsRegistry.forCode(settings.selectedLanguageCode)
                    }
                    val layoutDir = remember(settings.selectedLanguageCode) {
                        if (AppStringsRegistry.isRtl(settings.selectedLanguageCode)) {
                            LayoutDirection.Rtl
                        } else {
                            LayoutDirection.Ltr
                        }
                    }

                    LaunchedEffect(settings.selectedLanguageCode) {
                        val locale = Locale.forLanguageTag(settings.selectedLanguageCode.replace("_", "-"))
                        Locale.setDefault(locale)
                    }

                    val credentialManager = remember(context) { CredentialManager.create(context) }
                    val authScope = rememberCoroutineScope()

                    val performLogOut: () -> Unit = {
                        accountStore.logoutLocalAccount()
                        activeLocalAccount = null
                        GoogleAuthHelper.signOut(
                            credentialManager = credentialManager,
                            scope = authScope,
                            onBeforeAuthSignOut = { viewModel.prepareForSignOut() },
                            onSignOutComplete = {
                                bypassAuth = false
                                currentUser = null
                            }
                        )
                    }

                    CompositionLocalProvider(
                        LocalAppStrings provides localizedStrings,
                        LocalLayoutDirection provides layoutDir
                    ) {
                        MyApplicationTheme(darkTheme = settings.isDarkMode) {
                            val currentStrings = LocalAppStrings.current
                            val resolvedSignedIn = signedInUser != null || activeLocalAccount != null
                            val resolvedContact = signedInUser?.email ?: activeLocalAccount?.contact.orEmpty()
                            val resolvedDisplayName = signedInUser?.displayName
                                ?: activeLocalAccount?.fullName.orEmpty()
                            MainAppContent(
                                viewModel = viewModel,
                                signedInEmail = resolvedContact,
                                signedInDisplayName = resolvedDisplayName,
                                isSignedIn = resolvedSignedIn,
                                onRequestSignIn = { bypassAuth = false },
                                onLogOut = performLogOut,
                                onShareText = { text ->
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, text)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, currentStrings.sharePlatformChooser)
                                    startActivity(shareIntent)
                                },
                                onCopyText = { text ->
                                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Post Link", text)
                                    clipboard.setPrimaryClip(clip)
                                },
                                onOpenUrl = { url ->
                                    try {
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        startActivity(browserIntent)
                                    } catch (_: Exception) {
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(
    viewModel: PlatformViewModel,
    signedInEmail: String,
    signedInDisplayName: String,
    isSignedIn: Boolean,
    onRequestSignIn: () -> Unit,
    onLogOut: () -> Unit,
    onShareText: (String) -> Unit,
    onCopyText: (String) -> Unit,
    onOpenUrl: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    var activeSubScreen by remember { mutableStateOf<SubScreen?>(null) }
    var activeCommentPost by remember { mutableStateOf<PostModel?>(null) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showActivityCenter by remember { mutableStateOf(false) }
    var showEditIdentityDialog by remember { mutableStateOf(false) }
    var bypassRegistration by remember { mutableStateOf(false) }
    var createPostInitialText by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val posts by viewModel.posts.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val friends by viewModel.friends.collectAsState()
    val reviews by viewModel.reviews.collectAsState()
    val lastExportPath by viewModel.lastExportPath.collectAsState()
    val lastExportTimestamp by viewModel.lastExportTimestamp.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val uploadState by viewModel.uploadState.collectAsState()

    val friendsCount = friends.count { it.status == FriendStatus.FRIEND }
    val pendingFriendRequestCount = friends.count { it.status == FriendStatus.INCOMING_REQUEST }

    fun showSnackbar(message: String) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    if (isSignedIn && !userProfile.isProfileCreated && !bypassRegistration) {
        CompleteRegistrationScreen(
            currentProfile = userProfile,
            googleEmail = signedInEmail,
            googleName = signedInDisplayName,
            onSaveAccount = { name, userId, email, phone ->
                viewModel.createOrUpdateIdentity(name, userId, email, phone)
                showSnackbar(strings.identity.identitySavedSnackbar)
            },
            onLogOut = onLogOut,
            onCancelAndUsePlatform = { bypassRegistration = true }
        )
        return
    }

    BackHandler(enabled = activeSubScreen != null || selectedTab != 0) {
        if (activeSubScreen != null) {
            activeSubScreen = null
        } else if (selectedTab != 0) {
            viewModel.setSelectedTab(0)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (activeSubScreen == null) {
                BottomNavBar(
                    selectedTab = selectedTab,
                    onTabSelected = { tabIndex ->
                        viewModel.setSelectedTab(tabIndex)
                    }
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (activeSubScreen == SubScreen.NOTIFICATIONS) {
                NotificationsScreen(
                    notifications = notifications,
                    onBack = { activeSubScreen = null },
                    onMarkAllRead = {
                        viewModel.markNotificationsRead()
                        showSnackbar(strings.markAllReadSnackbar)
                    }
                )
            } else if (activeSubScreen == SubScreen.SETTINGS) {
                SettingsScreen(
                    settings = settings,
                    userPosts = posts.filter { it.isUserPost },
                    signedInEmail = signedInEmail.ifBlank { userProfile.email },
                    isSignedIn = isSignedIn,
                    onUpdateSettings = { viewModel.updateSettings(it) },
                    onDeletePost = { viewModel.deletePost(it) },
                    onOpenReviewsAndStorage = { activeSubScreen = SubScreen.REVIEWS_STORAGE },
                    onJoinCommunity = { onOpenUrl(LocalRepository.COMMUNITY_TELEGRAM_URL) },
                    onCopyCommunityUrl = {
                        onCopyText(LocalRepository.COMMUNITY_TELEGRAM_URL)
                        showSnackbar(strings.community.communityCopiedSnackbar)
                    },
                    onSignIn = {
                        activeSubScreen = null
                        onRequestSignIn()
                    },
                    onLogOut = onLogOut,
                    onBack = { activeSubScreen = null },
                    onShowSnackbar = { showSnackbar(it) }
                )
            } else if (activeSubScreen == SubScreen.REVIEWS_STORAGE) {
                val collectedJson = remember(userProfile, posts, friends, reviews, settings) {
                    viewModel.buildCollectedUserDataJson()
                }
                ReviewsAndStorageScreen(
                    userProfile = userProfile,
                    postsCount = posts.count { it.isUserPost },
                    friendsCount = friendsCount,
                    reviews = reviews,
                    collectedJson = collectedJson,
                    lastExportPath = lastExportPath,
                    lastExportTimestamp = lastExportTimestamp,
                    onSubmitReview = { rating, comment ->
                        viewModel.submitReview(rating, comment)
                    },
                    onUpdateReviewStatus = { reviewId, status ->
                        viewModel.updateReviewStatus(reviewId, status)
                    },
                    onTogglePinReview = { reviewId ->
                        viewModel.togglePinReview(reviewId)
                    },
                    onDeleteReview = { reviewId ->
                        viewModel.deleteReview(reviewId)
                    },
                    onExportUserData = { viewModel.exportCollectedUserData() },
                    onShareJsonText = { json -> onShareText(json) },
                    onCopyText = { text -> onCopyText(text) },
                    onShowSnackbar = { showSnackbar(it) },
                    onBack = { activeSubScreen = null }
                )
            } else if (activeSubScreen == SubScreen.AI_STUDIO) {
                AiStudioScreen(
                    aiRepository = viewModel.aiRepository,
                    selectedLanguageCode = settings.selectedLanguageCode,
                    onBack = { activeSubScreen = null },
                    onUseTextInPost = { generatedText ->
                        activeSubScreen = null
                        createPostInitialText = generatedText
                        viewModel.setSelectedTab(2)
                    },
                    onCopyText = { onCopyText(it) },
                    onShowSnackbar = { showSnackbar(it) }
                )
            } else if (activeSubScreen == SubScreen.FRIENDS) {
                FriendsScreen(
                    friends = friends,
                    onAcceptRequest = { user ->
                        viewModel.acceptFriendRequest(user.id)
                        showSnackbar(strings.friendAcceptedSnackbar(user.name))
                    },
                    onDeclineRequest = { user ->
                        viewModel.declineFriendRequest(user.id)
                        showSnackbar(strings.friendDeclinedSnackbar(user.name))
                    },
                    onSendRequest = { user ->
                        viewModel.sendFriendRequest(user.id)
                        showSnackbar(strings.friendRequestSentSnackbar(user.name))
                    },
                    onCancelRequest = { user ->
                        viewModel.cancelFriendRequest(user.id)
                        showSnackbar(strings.friendRequestCancelledSnackbar(user.name))
                    },
                    onUnfriend = { user ->
                        viewModel.unfriend(user.id)
                        showSnackbar(strings.unfriendedSnackbar(user.name))
                    },
                    onRemoveSuggestion = { user ->
                        viewModel.removeSuggestion(user.id)
                    },
                    onBack = { activeSubScreen = null }
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    AppHeader(
                        unreadNotificationCount = notifications.count { !it.isRead },
                        pendingFriendRequestCount = pendingFriendRequestCount,
                        onAiStudioClick = { activeSubScreen = SubScreen.AI_STUDIO },
                        onFriendsClick = { activeSubScreen = SubScreen.FRIENDS },
                        onNotificationClick = { activeSubScreen = SubScreen.NOTIFICATIONS }
                    )

                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "tabContent"
                    ) { tab ->
                        when (tab) {
                            0 -> HomeScreen(
                                posts = posts,
                                userAvatarUri = userProfile.avatarImageUri,
                                userAvatarColorHex = userProfile.avatarColorHex,
                                uploadState = uploadState,
                                friendsCount = friendsCount,
                                pendingRequestCount = pendingFriendRequestCount,
                                onRetryUpload = {
                                    viewModel.retryLastUpload {
                                        showSnackbar(strings.postPublishedSnackbar)
                                    }
                                },
                                onDismissUpload = { viewModel.dismissUploadState() },
                                onOpenFriends = { activeSubScreen = SubScreen.FRIENDS },
                                onOpenCreatePost = { viewModel.setSelectedTab(2) },
                                onJoinCommunity = { onOpenUrl(LocalRepository.COMMUNITY_TELEGRAM_URL) },
                                onCopyCommunityUrl = {
                                    onCopyText(LocalRepository.COMMUNITY_TELEGRAM_URL)
                                    showSnackbar(strings.community.communityCopiedSnackbar)
                                },
                                onLikePost = { viewModel.toggleLike(it) },
                                onCommentPost = { post -> activeCommentPost = post },
                                onSharePost = { post ->
                                    onCopyText("https://yourplatform.app/post/${post.id}")
                                    showSnackbar(strings.postLinkCopied)
                                },
                                onDeletePost = { viewModel.deletePost(it) }
                            )

                            1 -> WavesScreen(
                                onShareReel = { text -> onShareText(text) }
                            )

                            2 -> CreatePostScreen(
                                aiRepository = viewModel.aiRepository,
                                uploadState = uploadState,
                                selectedLanguageCode = settings.selectedLanguageCode,
                                initialText = createPostInitialText,
                                onSubmitPost = { text, type, uri, previewUri ->
                                    viewModel.submitPost(
                                        text = text,
                                        type = type,
                                        mediaUri = uri,
                                        previewUri = previewUri,
                                        onPublished = {
                                            createPostInitialText = ""
                                            showSnackbar(strings.postPublishedSnackbar)
                                        }
                                    )
                                },
                                onRetryUpload = {
                                    viewModel.retryLastUpload {
                                        createPostInitialText = ""
                                        showSnackbar(strings.postPublishedSnackbar)
                                    }
                                },
                                onDismissUpload = { viewModel.dismissUploadState() },
                                onShowSnackbar = { showSnackbar(it) }
                            )

                            3 -> SearchScreen(
                                searchQuery = searchQuery,
                                onQueryChange = { viewModel.setSearchQuery(it) },
                                posts = posts,
                                userAvatarUri = userProfile.avatarImageUri,
                                userAvatarColorHex = userProfile.avatarColorHex,
                                friends = friends,
                                onSendFriendRequest = { user ->
                                    viewModel.sendFriendRequest(user.id)
                                    showSnackbar(strings.friendRequestSentSnackbar(user.name))
                                },
                                onAcceptFriendRequest = { user ->
                                    viewModel.acceptFriendRequest(user.id)
                                    showSnackbar(strings.friendAcceptedSnackbar(user.name))
                                },
                                onCancelFriendRequest = { user ->
                                    viewModel.cancelFriendRequest(user.id)
                                    showSnackbar(strings.friendRequestCancelledSnackbar(user.name))
                                },
                                onOpenFriendsScreen = { activeSubScreen = SubScreen.FRIENDS },
                                onLikePost = { viewModel.toggleLike(it) },
                                onCommentPost = { post -> activeCommentPost = post },
                                onSharePost = { post ->
                                    onCopyText("https://yourplatform.app/post/${post.id}")
                                    showSnackbar(strings.postLinkCopied)
                                },
                                onDeletePost = { viewModel.deletePost(it) }
                            )

                            4 -> ProfileScreen(
                                userProfile = userProfile,
                                posts = posts,
                                selectedLanguageCode = settings.selectedLanguageCode,
                                onLikePost = { viewModel.toggleLike(it) },
                                onCommentPost = { post -> activeCommentPost = post },
                                onSharePost = { post ->
                                    onCopyText("https://yourplatform.app/post/${post.id}")
                                    showSnackbar(strings.postLinkCopied)
                                },
                                onDeletePost = { viewModel.deletePost(it) },
                                onUpdateCoverColor = { viewModel.updateCoverColor(it) },
                                onUpdateCoverUri = { viewModel.updateCoverUri(it) },
                                onUpdateAvatarColor = { viewModel.updateAvatarColor(it) },
                                onUpdateAvatarUri = { viewModel.updateAvatarUri(it) },
                                onUpdateBio = { viewModel.updateBio(it) },
                                aiRepository = viewModel.aiRepository,
                                friendsCount = friendsCount,
                                pendingFriendRequestCount = pendingFriendRequestCount,
                                onOpenFriends = { activeSubScreen = SubScreen.FRIENDS },
                                onOpenReviewsAndStorage = { activeSubScreen = SubScreen.REVIEWS_STORAGE },
                                onOpenEditIdentity = { showEditIdentityDialog = true },
                                onOpenQrCode = { showQrDialog = true },
                                onOpenActivityCenter = { showActivityCenter = true },
                                onOpenSettings = { activeSubScreen = SubScreen.SETTINGS },
                                isSignedIn = isSignedIn,
                                onSignInClick = onRequestSignIn,
                                onLogOut = onLogOut,
                                onShowSnackbar = { showSnackbar(it) }
                            )
                        }
                    }
                }
            }

            if (settings.isDimmingEnabled && settings.dimmingLevel > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = (settings.dimmingLevel / 100f) * 0.45f))
                )
            }
        }
    }

    if (activeCommentPost != null) {
        val currentPost = posts.find { it.id == activeCommentPost?.id } ?: activeCommentPost!!
        var aiSuggestions by remember(currentPost.id, settings.selectedLanguageCode) {
            mutableStateOf(strings.defaultCommentSuggestions)
        }

        LaunchedEffect(currentPost.id, settings.selectedLanguageCode) {
            try {
                val aiLang = AppStringsRegistry.aiLanguageName(settings.selectedLanguageCode)
                val suggestions = viewModel.aiRepository.generateCommentSuggestions(
                    postText = currentPost.postText,
                    languageName = aiLang,
                    fallbackSuggestions = strings.defaultCommentSuggestions
                )
                if (suggestions.isNotEmpty()) {
                    aiSuggestions = suggestions
                }
            } catch (_: Exception) {
            }
        }

        CommentBottomSheet(
            post = currentPost,
            aiSuggestions = aiSuggestions,
            onDismiss = { activeCommentPost = null },
            onAddComment = { commentText ->
                viewModel.addComment(currentPost.id, commentText)
                showSnackbar(strings.commentAddedSnackbar)
            }
        )
    }

    if (showQrDialog) {
        QrCodeDialog(
            userProfile = userProfile,
            onDismiss = { showQrDialog = false },
            onShare = {
                onShareText("https://yourplatform.app/u/${userProfile.handle.removePrefix("@")}")
                showQrDialog = false
            }
        )
    }

    if (showActivityCenter) {
        ActivityCenterSheet(onDismiss = { showActivityCenter = false })
    }

    if (showEditIdentityDialog) {
        CreateIdentityDialog(
            currentProfile = userProfile,
            isEditingExisting = true,
            onDismiss = { showEditIdentityDialog = false },
            onSaveIdentity = { name, userId, email, phone ->
                viewModel.createOrUpdateIdentity(name, userId, email, phone)
                showEditIdentityDialog = false
                showSnackbar(strings.identity.identitySavedSnackbar)
            }
        )
    }
}
