package com.example.ui.screens

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PostModel
import com.example.ui.components.PostCardItem
import com.example.ui.components.UploadProgressCard
import com.example.ui.theme.BrandPrimary
import com.example.util.LocalAppStrings
import com.example.viewmodel.PostUploadState

@Composable
fun HomeScreen(
    posts: List<PostModel>,
    uploadState: PostUploadState,
    userAvatarUri: String? = null,
    userAvatarColorHex: Long? = null,
    onRetryUpload: () -> Unit,
    onDismissUpload: () -> Unit,
    onOpenCreatePost: (initialMode: String?) -> Unit,
    onOpenPostInWaves: (postId: String) -> Unit,
    onLikePost: (String) -> Unit,
    onCommentPost: (PostModel) -> Unit,
    onSharePost: (PostModel) -> Unit,
    onDeletePost: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val studioStrings = strings.mediaStudio

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_feed_list"),
        contentPadding = PaddingValues(12.dp)
    ) {
        // Active upload status banner
        if (uploadState !is PostUploadState.Idle) {
            item {
                UploadProgressCard(
                    uploadState = uploadState,
                    onRetry = onRetryUpload,
                    onDismiss = onDismissUpload,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
        }

        // Clean Post Bar: Compose + Quick Photo, Reels/Video & Live Actions
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("quick_create_post_card")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenCreatePost(null) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BrandPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = strings.navCreatePost,
                                tint = BrandPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = strings.quickCreatePrompt,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                fontSize = 14.sp
                            ),
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onOpenCreatePost("gallery") }
                                .padding(horizontal = 6.dp)
                                .testTag("home_postbar_photo")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = studioStrings.postBarPhotoBtn,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = studioStrings.postBarPhotoBtn,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onOpenCreatePost("reels") }
                                .padding(horizontal = 6.dp)
                                .testTag("home_postbar_reels")
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideoCall,
                                contentDescription = studioStrings.postBarVideoReelsBtn,
                                tint = BrandPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = studioStrings.postBarVideoReelsBtn,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onOpenCreatePost("live") }
                                .padding(horizontal = 6.dp)
                                .testTag("home_postbar_live")
                        ) {
                            Icon(
                                imageVector = Icons.Default.LiveTv,
                                contentDescription = studioStrings.postBarLiveBtn,
                                tint = Color(0xFFE53935),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = studioStrings.postBarLiveBtn,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Clean Feed Items (Text, Photos, Videos, Reels, Live)
        items(posts, key = { it.id }) { post ->
            PostCardItem(
                post = post,
                onLikeClick = { onLikePost(post.id) },
                onCommentClick = { onCommentPost(post) },
                onShareClick = { onSharePost(post) },
                onDeleteClick = if (post.isUserPost) { { onDeletePost(post.id) } } else null,
                onOpenInWaves = if (post.isVideo || post.mediaType == "reels" || post.mediaType == "live" || post.isLiveStream) {
                    { onOpenPostInWaves(post.id) }
                } else null,
                authorAvatarUri = if (post.isUserPost) userAvatarUri else null,
                authorAvatarColorHex = if (post.isUserPost) userAvatarColorHex else null,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }
}
