package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ReviewModel
import com.example.model.ReviewStatus
import com.example.model.UserProfile
import com.example.ui.theme.BrandAccent
import com.example.ui.theme.BrandPrimary
import com.example.util.LocalAppStrings
import java.util.Locale

private enum class ReviewFilterTab {
    ALL, APPROVED, PENDING, HIDDEN
}

private const val ADMIN_REVIEW_PIN = "ADMIN2026"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewsAndStorageScreen(
    userProfile: UserProfile,
    reviews: List<ReviewModel>,
    onSubmitReview: (rating: Int, comment: String) -> Unit,
    onUpdateReviewStatus: (reviewId: String, status: ReviewStatus) -> Unit,
    onTogglePinReview: (reviewId: String) -> Unit,
    onDeleteReview: (reviewId: String) -> Unit,
    onShowSnackbar: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var rating by remember { mutableIntStateOf(5) }
    var reviewText by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(ReviewFilterTab.ALL) }
    var adminUnlockedManually by remember { mutableStateOf(false) }
    var showAdminUnlockDialog by remember { mutableStateOf(false) }
    var adminPinInput by remember { mutableStateOf("") }
    var adminPinError by remember { mutableStateOf<String?>(null) }

    val isAdminView = userProfile.isAdmin || adminUnlockedManually

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
                actions = {
                    if (!isAdminView) {
                        TextButton(
                            onClick = { showAdminUnlockDialog = true },
                            modifier = Modifier.testTag("open_admin_review_unlock_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = strings.reviewsStorage.adminUnlockBtn,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = strings.reviewsStorage.adminUnlockBtn,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
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
            // 1. User-Facing Platform Rating Summary (No Data Storage UI)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("user_rating_summary_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.reviewsStorage.userRatingSummaryTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = strings.reviewsStorage.userRatingOnlySubtitle,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                        fontSize = 12.sp
                                    )
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFFB300).copy(alpha = 0.16f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFFFB300),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = averageRatingText,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFCC8800)
                                    )
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            if (isAdminView) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF2E7D32).copy(alpha = 0.14f)
                                ) {
                                    Text(
                                        text = strings.reviewsStorage.adminModeActiveBadge,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Submit Star Rating Card (Visible to Users & Admin)
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
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clickable { rating = star }
                                        .testTag("review_star_$star")
                                ) {
                                    Icon(
                                        imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                        contentDescription = "Star $star",
                                        tint = if (star <= rating) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
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
                                onSubmitReview(rating, reviewText.trim())
                                reviewText = ""
                                rating = 5
                                onShowSnackbar(strings.reviewsStorage.reviewSubmittedSnackbar)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("submit_review_button")
                        ) {
                            Text(strings.reviewsStorage.submitReviewBtn, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 3. Detailed Reviews & Moderation List: ADMIN ONLY
            if (!isAdminView) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_only_reviews_notice_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = BrandPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = strings.reviewsStorage.adminOnlyNotice,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                                    fontSize = 12.sp
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(
                                onClick = { showAdminUnlockDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text(strings.reviewsStorage.adminUnlockBtn, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.testTag("admin_reviews_section")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.reviewsStorage.allReviewsTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
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
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
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

    if (showAdminUnlockDialog) {
        AlertDialog(
            onDismissRequest = {
                showAdminUnlockDialog = false
                adminPinError = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = BrandPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.reviewsStorage.adminUnlockBtn, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = adminPinInput,
                        onValueChange = {
                            adminPinInput = it
                            adminPinError = null
                        },
                        label = { Text(strings.reviewsStorage.adminPinLabel) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_pin_input")
                    )
                    if (adminPinError != null) {
                        Text(
                            text = adminPinError!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (adminPinInput.trim().equals(ADMIN_REVIEW_PIN, ignoreCase = true)) {
                            adminUnlockedManually = true
                            showAdminUnlockDialog = false
                            adminPinInput = ""
                            adminPinError = null
                        } else {
                            adminPinError = strings.reviewsStorage.adminWrongPinError
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    modifier = Modifier.testTag("admin_pin_confirm_button")
                ) {
                    Text(strings.reviewsStorage.adminUnlockBtn)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAdminUnlockDialog = false
                        adminPinError = null
                    }
                ) {
                    Text(strings.cancel)
                }
            }
        )
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

                IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
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
