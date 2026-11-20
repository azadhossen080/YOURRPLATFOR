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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ReviewModel
import com.example.model.ReviewStatus
import com.example.model.UserProfile
import com.example.ui.theme.BrandAccent
import com.example.ui.theme.BrandPrimary
import com.example.util.LocalAppStrings
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class ReviewFilterTab {
    ALL, APPROVED, PENDING, HIDDEN
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewsAndStorageScreen(
    userProfile: UserProfile,
    postsCount: Int,
    friendsCount: Int,
    reviews: List<ReviewModel>,
    collectedJson: String,
    lastExportPath: String?,
    lastExportTimestamp: Long?,
    onSubmitReview: (rating: Int, comment: String) -> Unit,
    onUpdateReviewStatus: (reviewId: String, status: ReviewStatus) -> Unit,
    onTogglePinReview: (reviewId: String) -> Unit,
    onDeleteReview: (reviewId: String) -> Unit,
    onExportUserData: () -> File?,
    onShareJsonText: (String) -> Unit,
    onCopyText: (String) -> Unit,
    onShowSnackbar: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var rating by remember { mutableIntStateOf(5) }
    var reviewText by remember { mutableStateOf("") }
    var showJsonPreview by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(ReviewFilterTab.ALL) }

    val filteredReviews = remember(reviews, selectedFilter) {
        val base = when (selectedFilter) {
            ReviewFilterTab.ALL -> reviews
            ReviewFilterTab.APPROVED -> reviews.filter { it.status == ReviewStatus.APPROVED }
            ReviewFilterTab.PENDING -> reviews.filter { it.status == ReviewStatus.PENDING }
            ReviewFilterTab.HIDDEN -> reviews.filter { it.status == ReviewStatus.HIDDEN }
        }
        base.sortedWith(compareByDescending<ReviewModel> { it.isPinned }.thenByDescending { it.timestamp })
    }

    val averageRatingText = remember(reviews) {
        if (reviews.isEmpty()) "0.0"
        else String.format(Locale.US, "%.1f", reviews.map { it.rating }.average())
    }

    val lastExportFormatted = remember(lastExportTimestamp) {
        if (lastExportTimestamp == null) null
        else SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(lastExportTimestamp))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.reviewsStorage.reviewsAndStorageTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("reviews_storage_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.back
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        modifier = modifier.testTag("reviews_storage_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Collected User Data Storage Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(BrandPrimary.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = BrandPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.reviewsStorage.storageSummaryTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${userProfile.name} • ${strings.identity.userIdPrefix} ${userProfile.displayUserId}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = BrandPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }

                        // Live collected counts
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            DataStatPill(label = strings.followersStat, value = userProfile.followersCount)
                            DataStatPill(label = strings.postsStat, value = "$postsCount")
                            DataStatPill(label = strings.friendsStat, value = "$friendsCount")
                            DataStatPill(label = strings.reviewsStorage.allReviewsTitle.substringBefore(" "), value = "${reviews.size}")
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        // Cloud Firestore & Local JSON Storage Summary
                        Text(
                            text = strings.reviewsStorage.storageLinkLabel,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = BrandPrimary
                            )
                        )

                        Text(
                            text = strings.reviewsStorage.storageNoticeText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                lineHeight = 17.sp,
                                fontSize = 11.sp
                            )
                        )

                        Text(
                            text = "${strings.reviewsStorage.lastExportedLabel} ${lastExportFormatted ?: strings.reviewsStorage.notExportedYet}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )
                        if (!lastExportPath.isNullOrBlank()) {
                            Text(
                                text = lastExportPath,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                )
                            )
                        }

                        // Export & Share Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val file = onExportUserData()
                                    if (file != null) {
                                        onShowSnackbar(strings.reviewsStorage.exportSuccessSnackbar(file.name))
                                    } else {
                                        onShowSnackbar(strings.reviewsStorage.exportFailedSnackbar)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_user_data_button")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.reviewsStorage.collectExportJsonBtn, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onShareJsonText(collectedJson) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("share_user_data_button")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.reviewsStorage.shareBackupDataBtn, fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    onCopyText(collectedJson)
                                    onShowSnackbar(strings.copiedSnackbar)
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(0.8f)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.reviewsStorage.copyJsonBtn, fontSize = 11.sp)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { showJsonPreview = !showJsonPreview },
                                modifier = Modifier.testTag("toggle_json_preview_button")
                            ) {
                                Text(strings.reviewsStorage.jsonPreviewTitle, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (showJsonPreview) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = collectedJson.take(1500) + if (collectedJson.length > 1500) "\n..." else "",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    ),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Write a Review Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = strings.reviewsStorage.writeReviewTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = strings.reviewsStorage.ratingLabel,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            for (star in 1..5) {
                                Icon(
                                    imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                    contentDescription = "Star $star",
                                    tint = if (star <= rating) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clickable { rating = star }
                                        .testTag("review_star_$star")
                                )
                            }
                        }

                        OutlinedTextField(
                            value = reviewText,
                            onValueChange = { reviewText = it },
                            placeholder = { Text(strings.reviewsStorage.reviewInputPlaceholder) },
                            minLines = 2,
                            maxLines = 4,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("review_comment_input")
                        )

                        Button(
                            onClick = {
                                if (reviewText.isBlank()) {
                                    onShowSnackbar(strings.reviewsStorage.reviewEmptyError)
                                } else {
                                    onSubmitReview(rating, reviewText.trim())
                                    reviewText = ""
                                    rating = 5
                                    onShowSnackbar(strings.reviewsStorage.reviewSubmittedSnackbar)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("submit_review_button")
                        ) {
                            Text(strings.reviewsStorage.submitReviewBtn, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 3. All Reviews Header & Filters
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.reviewsStorage.allReviewsTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFFB300).copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = strings.reviewsStorage.avgRatingLabel(averageRatingText),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFCC8800),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = BrandPrimary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = strings.reviewsStorage.totalReviewsLabel(reviews.size),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedFilter == ReviewFilterTab.ALL,
                            onClick = { selectedFilter = ReviewFilterTab.ALL },
                            label = { Text(strings.reviewsStorage.filterAll, fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedFilter == ReviewFilterTab.APPROVED,
                            onClick = { selectedFilter = ReviewFilterTab.APPROVED },
                            label = { Text(strings.reviewsStorage.filterApproved, fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedFilter == ReviewFilterTab.PENDING,
                            onClick = { selectedFilter = ReviewFilterTab.PENDING },
                            label = { Text(strings.reviewsStorage.filterPending, fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedFilter == ReviewFilterTab.HIDDEN,
                            onClick = { selectedFilter = ReviewFilterTab.HIDDEN },
                            label = { Text(strings.reviewsStorage.filterHidden, fontSize = 11.sp) }
                        )
                    }
                }
            }

            if (filteredReviews.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.reviewsStorage.noReviewsYet,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                )
                            )
                        }
                    }
                }
            } else {
                items(filteredReviews, key = { it.id }) { review ->
                    ReviewControlCard(
                        review = review,
                        onApprove = { onUpdateReviewStatus(review.id, ReviewStatus.APPROVED) },
                        onHide = { onUpdateReviewStatus(review.id, ReviewStatus.HIDDEN) },
                        onTogglePin = { onTogglePinReview(review.id) },
                        onDelete = { onDeleteReview(review.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DataStatPill(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = BrandPrimary.copy(alpha = 0.08f)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = BrandPrimary
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ReviewControlCard(
    review: ReviewModel,
    onApprove: () -> Unit,
    onHide: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalAppStrings.current
    val (statusLabel, statusColor) = when (review.status) {
        ReviewStatus.APPROVED -> strings.reviewsStorage.statusApproved to Color(0xFF2E7D32)
        ReviewStatus.PENDING -> strings.reviewsStorage.statusPending to Color(0xFFED6C02)
        ReviewStatus.HIDDEN -> strings.reviewsStorage.statusHidden to BrandAccent
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("review_card_${review.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = review.userName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${strings.identity.userIdPrefix} ${review.userId} • ${review.timeAgo}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (review.isPinned) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = BrandPrimary.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = strings.reviewsStorage.pinnedBadge,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusColor.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = statusLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                for (i in 1..5) {
                    Icon(
                        imageVector = if (i <= review.rating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = null,
                        tint = if (i <= review.rating) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = review.comment,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (review.status != ReviewStatus.APPROVED) {
                        TextButton(onClick = onApprove) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.reviewsStorage.approveBtn, fontSize = 11.sp)
                        }
                    }
                    if (review.status != ReviewStatus.HIDDEN) {
                        TextButton(onClick = onHide) {
                            Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.reviewsStorage.hideBtn, fontSize = 11.sp)
                        }
                    }
                    TextButton(onClick = onTogglePin) {
                        Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (review.isPinned) strings.reviewsStorage.unpinBtn else strings.reviewsStorage.pinBtn, fontSize = 11.sp)
                    }
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = strings.delete,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
