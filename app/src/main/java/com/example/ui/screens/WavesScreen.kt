package com.example.ui.screens

import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.widget.VideoView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PauseCircleFilled
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.model.ColorFilterMode
import com.example.model.DEFAULT_MOJI_STICKERS
import com.example.model.LiveFloatingMoji
import com.example.model.PostModel
import com.example.model.TransitionEffect
import com.example.model.UserProfile
import com.example.ui.components.AppliedMediaDecorationsOverlay
import com.example.ui.components.FloatingMojiStage
import com.example.ui.components.TransitionEffectPreviewContainer
import com.example.ui.theme.BrandAccent
import com.example.ui.theme.BrandPrimary
import com.example.util.LocalAppStrings
import java.util.UUID
import kotlin.random.Random

private data class WavesReelEntry(
    val id: String,
    val title: String,
    val author: String,
    val caption: String,
    val song: String,
    val likes: Int,
    val isLiked: Boolean,
    val comments: Int,
    val mediaUri: String?,
    val thumbnailUri: String?,
    val isVideoMedia: Boolean,
    val colorFilter: String,
    val transitionEffect: String,
    val mojiStickers: List<String>,
    val playbackSpeed: Float,
    val isMuted: Boolean,
    val trimStartFraction: Float,
    val isLiveStream: Boolean,
    val gradient: List<Color>,
    val sourcePost: PostModel? = null
)

@Composable
fun WavesScreen(
    posts: List<PostModel>,
    userProfile: UserProfile,
    focusedPostId: String?,
    selectedLanguageCode: String,
    onLikePost: (String) -> Unit,
    onCommentPost: (PostModel) -> Unit,
    onSharePost: (PostModel) -> Unit,
    onShareReelText: (String) -> Unit,
    onOpenReelsCreator: (mode: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val studioStrings = strings.mediaStudio

    val showcaseGradients = remember {
        listOf(
            listOf(Color(0xFF1E1E2C), Color(0xFF5352ED), Color(0xFF2C1E4A)),
            listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)),
            listOf(Color(0xFF3A1C71), Color(0xFFD76D77), Color(0xFFFFAF7B))
        )
    }

    val platformReels = remember(posts, userProfile.handle, showcaseGradients) {
        posts.filter { post ->
            post.isVideo || post.mediaType == "reels" || post.mediaType == "live" || post.isLiveStream
        }.mapIndexed { idx, post ->
            val handle = if (post.isUserPost && userProfile.handle.isNotBlank()) {
                userProfile.handle
            } else {
                "@${post.userName.lowercase().replace("\\s+".toRegex(), "_")}"
            }
            WavesReelEntry(
                id = post.id,
                title = if (post.isLiveStream) studioStrings.liveBadge else "${strings.navWaves} #${idx + 1}",
                author = handle,
                caption = post.postText,
                song = "Original Audio - ${post.userName}",
                likes = post.likeCount,
                isLiked = post.isLiked,
                comments = post.comments.size,
                mediaUri = post.mediaUri,
                thumbnailUri = post.thumbnailUri,
                isVideoMedia = post.isVideo && !post.mediaUri.isNullOrBlank(),
                colorFilter = post.colorFilter,
                transitionEffect = post.transitionEffect,
                mojiStickers = post.mojiStickers,
                playbackSpeed = post.playbackSpeed,
                isMuted = post.isMuted,
                trimStartFraction = post.trimStartFraction,
                isLiveStream = post.isLiveStream || post.mediaType == "live",
                gradient = showcaseGradients[idx % showcaseGradients.size],
                sourcePost = post
            )
        }
    }

    val showcaseReels = remember(showcaseGradients) {
        listOf(
            WavesReelEntry(
                id = "demo_1",
                title = "Waves Video #1",
                author = "@yourplatform",
                caption = "YOURPLATFORM এ প্রথম রিলস ভিডিও শেয়ার করলাম! কেমন লাগলো জানাবেন 🌊✨ #waves #viral",
                song = "Original Audio - YOURPLATFORM Studio",
                likes = 142,
                isLiked = false,
                comments = 28,
                mediaUri = null,
                thumbnailUri = null,
                isVideoMedia = false,
                colorFilter = ColorFilterMode.VIVID.id,
                transitionEffect = TransitionEffect.FADE.id,
                mojiStickers = listOf("🌊", "✨"),
                playbackSpeed = 1.0f,
                isMuted = false,
                trimStartFraction = 0f,
                isLiveStream = false,
                gradient = showcaseGradients[0]
            ),
            WavesReelEntry(
                id = "demo_2",
                title = "Waves Video #2",
                author = "@techbangla",
                caption = "নতুন টেকনোলজি এবং মোবাইল অ্যাপ ট্রেন্ডস ২০২৬ 📱🚀 #tech #innovation",
                song = "Trending Beats - Sound Studio",
                likes = 320,
                isLiked = false,
                comments = 45,
                mediaUri = null,
                thumbnailUri = null,
                isVideoMedia = false,
                colorFilter = ColorFilterMode.COOL.id,
                transitionEffect = TransitionEffect.ZOOM_IN.id,
                mojiStickers = listOf("🚀", "🔥"),
                playbackSpeed = 1.0f,
                isMuted = false,
                trimStartFraction = 0f,
                isLiveStream = false,
                gradient = showcaseGradients[1]
            ),
            WavesReelEntry(
                id = "demo_3",
                title = "Waves Video #3",
                author = "@explore_nature",
                caption = "মেঘের দেশে এক বিকেল ⛅️ মনোরম প্রাকৃতিক দৃশ্য। #nature #peace",
                song = "Calm Melodies - Acoustic Vibes",
                likes = 512,
                isLiked = false,
                comments = 64,
                mediaUri = null,
                thumbnailUri = null,
                isVideoMedia = false,
                colorFilter = ColorFilterMode.WARM.id,
                transitionEffect = TransitionEffect.SLIDE_UP.id,
                mojiStickers = listOf("✨", "❤️"),
                playbackSpeed = 1.0f,
                isMuted = false,
                trimStartFraction = 0f,
                isLiveStream = false,
                gradient = showcaseGradients[2]
            )
        )
    }

    val allReels = remember(platformReels, showcaseReels) {
        platformReels + showcaseReels
    }

    val pagerState = rememberPagerState(pageCount = { allReels.size })
    val demoLikedMap = remember { mutableStateMapOf<String, Boolean>() }
    val isPlayingMap = remember { mutableStateMapOf<String, Boolean>() }
    val isMutedOverrideMap = remember { mutableStateMapOf<String, Boolean>() }
    val floatingMojis = remember { mutableStateListOf<LiveFloatingMoji>() }
    var activeTransitionTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(focusedPostId, allReels) {
        if (!focusedPostId.isNullOrBlank()) {
            val targetIdx = allReels.indexOfFirst { it.id == focusedPostId }
            if (targetIdx >= 0 && targetIdx != pagerState.currentPage) {
                pagerState.animateScrollToPage(targetIdx)
            }
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        activeTransitionTrigger++
    }

    fun launchFloatingMoji(emoji: String) {
        floatingMojis.add(
            LiveFloatingMoji(
                id = UUID.randomUUID().toString(),
                emoji = emoji,
                xFraction = Random.nextFloat()
            )
        )
        if (floatingMojis.size > 16) {
            floatingMojis.removeAt(0)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("waves_vertical_pager")
        ) { page ->
            val reel = allReels[page]
            val isCurrentPage = pagerState.currentPage == page
            val isLiked = if (reel.sourcePost != null) reel.isLiked else (demoLikedMap[reel.id] ?: false)
            val displayLikeCount = if (reel.sourcePost != null) reel.likes else (reel.likes + if (isLiked) 1 else 0)
            val isPlaying = isPlayingMap[reel.id] ?: true
            val isMuted = isMutedOverrideMap[reel.id] ?: reel.isMuted
            val filterMode = remember(reel.colorFilter) { ColorFilterMode.fromId(reel.colorFilter) }
            val transitionMode = remember(reel.transitionEffect) { TransitionEffect.fromId(reel.transitionEffect) }

            val infiniteTransition = rememberInfiniteTransition(label = "discRotation")
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(4000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "discAngle"
            )

            TransitionEffectPreviewContainer(
                effect = if (isCurrentPage) transitionMode else TransitionEffect.NONE,
                triggerKey = if (isCurrentPage) activeTransitionTrigger else 0,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.verticalGradient(reel.gradient))
                        .clickable { isPlayingMap[reel.id] = !isPlaying }
                ) {
                    if (reel.isVideoMedia && !reel.mediaUri.isNullOrBlank()) {
                        ReelVideoPlayerSurface(
                            videoUri = reel.mediaUri,
                            thumbnailUri = reel.thumbnailUri,
                            colorFilterMode = filterMode,
                            shouldPlay = isCurrentPage && isPlaying,
                            isMuted = isMuted,
                            playbackSpeed = reel.playbackSpeed,
                            trimStartFraction = reel.trimStartFraction
                        )
                    } else if (!reel.thumbnailUri.isNullOrBlank() || !reel.mediaUri.isNullOrBlank()) {
                        AsyncImage(
                            model = reel.thumbnailUri ?: reel.mediaUri,
                            contentDescription = reel.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            colorFilter = filterMode.colorFilter()
                        )
                    }

                    AppliedMediaDecorationsOverlay(
                        colorFilterId = reel.colorFilter,
                        transitionEffectId = reel.transitionEffect,
                        mojiStickers = reel.mojiStickers,
                        isLiveStream = reel.isLiveStream,
                        playbackSpeed = reel.playbackSpeed,
                        isMuted = isMuted,
                        languageCode = selectedLanguageCode
                    )

                    if (!isPlaying || !reel.isVideoMedia) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.PlayCircleFilled else Icons.Filled.PauseCircleFilled,
                                contentDescription = if (isPlaying) "Playing" else "Paused",
                                tint = Color.White.copy(alpha = if (isPlaying) 0.55f else 0.92f),
                                modifier = Modifier.size(68.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = reel.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = if (isPlaying) strings.tapToPause else strings.pausedTapToPlay,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.78f),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    // Right-side Interactive Action Column
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 12.dp, bottom = 78.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = {
                                    if (reel.sourcePost != null) {
                                        onLikePost(reel.sourcePost.id)
                                    } else {
                                        demoLikedMap[reel.id] = !isLiked
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                                    .testTag("reel_like_${reel.id}")
                            ) {
                                Icon(
                                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "Like",
                                    tint = if (isLiked) BrandAccent else Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Text(
                                text = "$displayLikeCount",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = {
                                    if (reel.sourcePost != null) {
                                        onCommentPost(reel.sourcePost)
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                                    .testTag("reel_comment_${reel.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ChatBubbleOutline,
                                    contentDescription = "Comments",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Text(
                                text = "${reel.comments}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = {
                                    if (reel.sourcePost != null) {
                                        onSharePost(reel.sourcePost)
                                    } else {
                                        onShareReelText(reel.caption)
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                                    .testTag("reel_share_${reel.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Share,
                                    contentDescription = strings.shareLabel,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Text(
                                text = strings.shareLabel,
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }

                        IconButton(
                            onClick = { isMutedOverrideMap[reel.id] = !isMuted },
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                                .testTag("reel_mute_${reel.id}")
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = if (isMuted) studioStrings.muteAudioLabel else studioStrings.unmuteAudioLabel,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .rotate(if (isPlaying) rotation else 0f)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Audiotrack,
                                contentDescription = "Audio",
                                tint = BrandPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Bottom Caption & Live Moji Reaction Bar
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                                )
                            )
                            .padding(start = 14.dp, end = 14.dp, bottom = 14.dp, top = 16.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth(0.78f)) {
                            Text(
                                text = reel.author,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = reel.caption,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White.copy(alpha = 0.92f),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                ),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Audiotrack,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = reel.song,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Tap Moji Reaction Strip inside Waves player
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DEFAULT_MOJI_STICKERS.take(8).forEach { emoji ->
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.48f))
                                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                                        .clickable { launchFloatingMoji(emoji) }
                                        .testTag("waves_moji_${emoji.hashCode()}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = emoji, fontSize = 18.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Moji Reactions Layer across Waves
        FloatingMojiStage(floatingMojis = floatingMojis)

        // Top Overlay Header: Quick Create Reels / Record / Go Live + Count Indicator
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.62f))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                    .clickable { onOpenReelsCreator("reels") }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("waves_create_reel_pill")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VideoCall, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(studioStrings.openCreateReelBtn, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.62f))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                    .clickable { onOpenReelsCreator("record") }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("waves_record_pill")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FiberManualRecord, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(studioStrings.recordingModeBtn, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFE53935).copy(alpha = 0.88f))
                    .clickable { onOpenReelsCreator("live") }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("waves_live_pill")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LiveTv, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(studioStrings.goLiveBtn, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ReelVideoPlayerSurface(
    videoUri: String,
    thumbnailUri: String?,
    colorFilterMode: ColorFilterMode,
    shouldPlay: Boolean,
    isMuted: Boolean,
    playbackSpeed: Float,
    trimStartFraction: Float
) {
    var mediaPlayerRef by remember(videoUri) { mutableStateOf<MediaPlayer?>(null) }
    var videoViewRef by remember(videoUri) { mutableStateOf<VideoView?>(null) }
    var videoPrepared by remember(videoUri) { mutableStateOf(false) }

    LaunchedEffect(shouldPlay, isMuted, playbackSpeed, videoPrepared) {
        val mp = mediaPlayerRef
        val vv = videoViewRef
        if (videoPrepared && mp != null && vv != null) {
            try {
                val vol = if (isMuted) 0f else 1f
                mp.setVolume(vol, vol)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    mp.playbackParams = mp.playbackParams.setSpeed(playbackSpeed.coerceIn(0.5f, 2.0f))
                }
                if (shouldPlay && !vv.isPlaying) {
                    vv.start()
                } else if (!shouldPlay && vv.isPlaying) {
                    vv.pause()
                }
            } catch (_: Exception) {
            }
        }
    }

    DisposableEffect(videoUri) {
        onDispose {
            try {
                videoViewRef?.stopPlayback()
            } catch (_: Exception) {
            }
            mediaPlayerRef = null
            videoViewRef = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!thumbnailUri.isNullOrBlank()) {
            AsyncImage(
                model = thumbnailUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                colorFilter = colorFilterMode.colorFilter()
            )
        }

        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    setVideoURI(Uri.parse(videoUri))
                    setOnPreparedListener { mp ->
                        mediaPlayerRef = mp
                        videoPrepared = true
                        mp.isLooping = true
                        val vol = if (isMuted) 0f else 1f
                        mp.setVolume(vol, vol)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && playbackSpeed != 1f) {
                            try {
                                mp.playbackParams = mp.playbackParams.setSpeed(playbackSpeed.coerceIn(0.5f, 2.0f))
                            } catch (_: Exception) {
                            }
                        }
                        if (trimStartFraction > 0f && mp.duration > 0) {
                            seekTo((mp.duration * trimStartFraction).toInt())
                        }
                        if (shouldPlay) start()
                    }
                    setOnErrorListener { _, _, _ -> true }
                    videoViewRef = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
