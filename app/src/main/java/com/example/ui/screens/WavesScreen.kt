package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PauseCircleFilled
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrandAccent
import com.example.ui.theme.BrandPrimary
import com.example.util.LocalAppStrings

data class ReelItem(
    val id: String,
    val title: String,
    val author: String,
    val caption: String,
    val song: String,
    val likes: Int,
    val comments: Int,
    val gradient: List<Color>
)

@Composable
fun WavesScreen(
    onShareReel: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val reels = remember {
        listOf(
            ReelItem(
                id = "1",
                title = "Waves Video #1",
                author = "@yourplatform",
                caption = "YOURPLATFORM এ প্রথম রিলস ভিডিও শেয়ার করলাম! কেমন লাগলো জানাবেন 🌊✨ #waves #viral",
                song = "Original Audio - YOURPLATFORM Studio",
                likes = 142,
                comments = 28,
                gradient = listOf(Color(0xFF1E1E2C), Color(0xFF5352ED), Color(0xFF2C1E4A))
            ),
            ReelItem(
                id = "2",
                title = "Waves Video #2",
                author = "@techbangla",
                caption = "নতুন টেকনোলজি এবং মোবাইল অ্যাপ ট্রেন্ডস ২০২৬ 📱🚀 #tech #innovation",
                song = "Trending Beats - Sound Studio",
                likes = 320,
                comments = 45,
                gradient = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
            ),
            ReelItem(
                id = "3",
                title = "Waves Video #3",
                author = "@explore_nature",
                caption = "মেঘের দেশে এক বিকেল ⛅️ মনোরম প্রাকৃতিক দৃশ্য। #nature #peace",
                song = "Calm Melodies - Acoustic Vibes",
                likes = 512,
                comments = 64,
                gradient = listOf(Color(0xFF3A1C71), Color(0xFFD76D77), Color(0xFFFFAF7B))
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { reels.size })
    val likedMap = remember { mutableStateMapOf<String, Boolean>() }
    val isPlayingMap = remember { mutableStateMapOf<String, Boolean>() }

    VerticalPager(
        state = pagerState,
        modifier = modifier
            .fillMaxSize()
            .testTag("waves_vertical_pager")
    ) { page ->
        val reel = reels[page]
        val isLiked = likedMap[reel.id] ?: false
        val isPlaying = isPlayingMap[reel.id] ?: true

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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(reel.gradient))
                .clickable {
                    isPlayingMap[reel.id] = !isPlaying
                }
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.PlayCircleFilled else Icons.Filled.PauseCircleFilled,
                    contentDescription = if (isPlaying) "Playing" else "Paused",
                    tint = Color.White.copy(alpha = if (isPlaying) 0.6f else 0.9f),
                    modifier = Modifier.size(68.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
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
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { likedMap[reel.id] = !isLiked },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                            .testTag("reel_like_${reel.id}")
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) BrandAccent else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "${reel.likes + if (isLiked) 1 else 0}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { /* View comments */ },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Comments",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
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
                        onClick = { onShareReel(reel.caption) },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = strings.shareLabel,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = strings.shareLabel,
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }

                // Rotating sound disc
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .rotate(if (isPlaying) rotation else 0f)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
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

            // Bottom Caption & Creator Info
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(0.78f)
                    .padding(start = 14.dp, bottom = 24.dp)
            ) {
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
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    ),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Audiotrack,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = reel.song,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}
