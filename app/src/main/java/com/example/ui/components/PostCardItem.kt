package com.example.ui.components

import android.net.Uri
import android.os.Build
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.model.ColorFilterMode
import com.example.model.PostModel
import com.example.ui.theme.BrandAccent
import com.example.ui.theme.BrandPrimary
import com.example.util.LocalAppStrings

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PostCardItem(
    post: PostModel,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null,
    onOpenInWaves: (() -> Unit)? = null,
    authorAvatarUri: String? = null,
    authorAvatarColorHex: Long? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var menuExpanded by remember { mutableStateOf(false) }
    var showMediaViewer by remember { mutableStateOf(false) }

    val colorFilterMode = remember(post.colorFilter) { ColorFilterMode.fromId(post.colorFilter) }
    val isReelOrVideo = post.isVideo || post.mediaType == "reels" || post.mediaType == "live" || post.isLiveStream

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("post_card_${post.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(authorAvatarColorHex?.let { Color(it) } ?: BrandPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    if (!authorAvatarUri.isNullOrBlank()) {
                        AsyncImage(
                            model = authorAvatarUri,
                            contentDescription = post.userName,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = post.userName.take(1),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.userName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        VerifiedAndVipBadges(
                            isVerified = post.authorVerified,
                            isVip = post.authorVip,
                            compact = true
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = post.timeAgo,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        )
                        if (post.locationName.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = BrandPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = post.locationName,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = BrandPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                        if (post.ageRestriction.isNotBlank() && post.ageRestriction != "All Ages") {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = BrandAccent.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = post.ageRestriction,
                                    color = BrandAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                if (isReelOrVideo && onOpenInWaves != null) {
                    FilledTonalButton(
                        onClick = onOpenInWaves,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = BrandPrimary.copy(alpha = 0.14f),
                            contentColor = BrandPrimary
                        ),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("watch_in_waves_${post.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = strings.mediaStudio.watchInWavesBtn,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = strings.mediaStudio.watchInWavesBtn,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (onDeleteClick != null) {
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(strings.deletePostMenu, color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = strings.delete,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteClick()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (post.postTitle.isNotBlank()) {
                Text(
                    text = post.postTitle,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // Post Text
            Text(
                text = post.postText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 20.sp
                )
            )

            if (post.playlistName.isNotBlank() || post.collaborators.isNotEmpty() || post.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (post.playlistName.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BrandPrimary.copy(alpha = 0.1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlaylistPlay,
                                    contentDescription = null,
                                    tint = BrandPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = post.playlistName,
                                    color = BrandPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    post.collaborators.forEach { collab ->
                        Text(
                            text = collab,
                            color = BrandAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    post.tags.forEach { tag ->
                        Text(
                            text = "#$tag",
                            color = BrandPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Post Media
            if (post.mediaType == "gradient") {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(BrandPrimary, BrandAccent)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_app_logo),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "YOURPLATFORM",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = "Make your any choose....",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    AppliedMediaDecorationsOverlay(
                        colorFilterId = post.colorFilter,
                        transitionEffectId = post.transitionEffect,
                        mojiStickers = post.mojiStickers,
                        isLiveStream = post.isLiveStream,
                        playbackSpeed = post.playbackSpeed,
                        isMuted = post.isMuted,
                        overlayText = post.overlayText,
                        overlayColorHex = post.overlayColorHex,
                        soundName = post.soundName,
                        timerDurationSec = post.timerDurationSec
                    )
                }
            } else if (!post.mediaUri.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                val displayModel = post.thumbnailUri ?: post.mediaUri
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E1E2C))
                        .clickable {
                            if (isReelOrVideo && onOpenInWaves != null) {
                                onOpenInWaves()
                            } else {
                                showMediaViewer = true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = displayModel,
                        contentDescription = "Post Media",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        colorFilter = colorFilterMode.colorFilter()
                    )

                    if (isReelOrVideo) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.25f))
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.PlayCircleFilled,
                                contentDescription = strings.tapToPlayVideo,
                                tint = Color.White.copy(alpha = 0.94f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = strings.tapToPlayVideo,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.65f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = strings.videoPostLabel,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    AppliedMediaDecorationsOverlay(
                        colorFilterId = post.colorFilter,
                        transitionEffectId = post.transitionEffect,
                        mojiStickers = post.mojiStickers,
                        isLiveStream = post.isLiveStream,
                        playbackSpeed = post.playbackSpeed,
                        isMuted = post.isMuted,
                        overlayText = post.overlayText,
                        overlayColorHex = post.overlayColorHex,
                        soundName = post.soundName,
                        timerDurationSec = post.timerDurationSec
                    )
                }
            } else if (isReelOrVideo) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF1F1D36), BrandPrimary.copy(alpha = 0.85f))
                            )
                        )
                        .clickable { onOpenInWaves?.invoke() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.PlayCircleFilled,
                            contentDescription = "Reels",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (post.isLiveStream) strings.mediaStudio.liveBadge else strings.wavesReelContent,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    AppliedMediaDecorationsOverlay(
                        colorFilterId = post.colorFilter,
                        transitionEffectId = post.transitionEffect,
                        mojiStickers = post.mojiStickers,
                        isLiveStream = post.isLiveStream,
                        playbackSpeed = post.playbackSpeed,
                        isMuted = post.isMuted,
                        overlayText = post.overlayText,
                        overlayColorHex = post.overlayColorHex,
                        soundName = post.soundName,
                        timerDurationSec = post.timerDurationSec
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )

            // Actions row: Like, Comment, Share
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val likeColor by animateColorAsState(
                    targetValue = if (post.isLiked) BrandPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    animationSpec = spring(),
                    label = "likeColor"
                )

                TextButton(
                    onClick = onLikeClick,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("post_like_button_${post.id}")
                ) {
                    Icon(
                        imageVector = if (post.isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                        contentDescription = "Like",
                        tint = likeColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.likesCount(post.likeCount),
                        color = likeColor,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                    )
                }

                TextButton(
                    onClick = onCommentClick,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("post_comment_button_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Comment",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.commentsCount(post.comments.size),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                    )
                }

                TextButton(
                    onClick = onShareClick,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("post_share_button_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = strings.shareLabel,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.shareLabel,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                    )
                }
            }
        }
    }

    if (showMediaViewer && !post.mediaUri.isNullOrBlank()) {
        Dialog(
            onDismissRequest = { showMediaViewer = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.Black
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (post.isVideo) {
                        AndroidView(
                            factory = { ctx ->
                                VideoView(ctx).apply {
                                    setVideoURI(Uri.parse(post.mediaUri))
                                    val controller = MediaController(ctx)
                                    controller.setAnchorView(this)
                                    setMediaController(controller)
                                    setOnPreparedListener { mp ->
                                        mp.isLooping = true
                                        val vol = if (post.isMuted) 0f else 1f
                                        mp.setVolume(vol, vol)
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && post.playbackSpeed != 1f) {
                                            try {
                                                mp.playbackParams = mp.playbackParams.setSpeed(post.playbackSpeed)
                                            } catch (_: Exception) {
                                            }
                                        }
                                        val duration = mp.duration
                                        if (duration > 0 && post.trimStartFraction > 0f) {
                                            seekTo((duration * post.trimStartFraction).toInt())
                                        }
                                        start()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(360.dp)
                        )
                    } else {
                        AsyncImage(
                            model = post.mediaUri,
                            contentDescription = "Full Photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp),
                            contentScale = ContentScale.Fit,
                            colorFilter = colorFilterMode.colorFilter()
                        )
                    }

                    AppliedMediaDecorationsOverlay(
                        colorFilterId = post.colorFilter,
                        transitionEffectId = post.transitionEffect,
                        mojiStickers = post.mojiStickers,
                        isLiveStream = post.isLiveStream,
                        playbackSpeed = post.playbackSpeed,
                        isMuted = post.isMuted,
                        overlayText = post.overlayText,
                        overlayColorHex = post.overlayColorHex,
                        soundName = post.soundName,
                        timerDurationSec = post.timerDurationSec
                    )

                    IconButton(
                        onClick = { showMediaViewer = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = strings.close,
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
