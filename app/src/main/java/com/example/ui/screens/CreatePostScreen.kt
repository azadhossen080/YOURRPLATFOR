package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.AiRepository
import com.example.model.ColorFilterMode
import com.example.model.MediaEditDraft
import com.example.model.TransitionEffect
import com.example.ui.components.AppliedMediaDecorationsOverlay
import com.example.ui.components.LiveAndRecordStudioDialog
import com.example.ui.components.MediaEditControlsPanel
import com.example.ui.components.TransitionEffectPreviewContainer
import com.example.ui.components.UploadProgressCard
import com.example.ui.theme.BrandAccent
import com.example.ui.theme.BrandPrimary
import com.example.util.AppStringsRegistry
import com.example.util.LocalAppStrings
import com.example.util.MediaProcessor
import com.example.viewmodel.PostUploadState
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Composable
fun CreatePostScreen(
    aiRepository: AiRepository,
    uploadState: PostUploadState,
    selectedLanguageCode: String,
    initialText: String = "",
    initialStudioMode: String? = null,
    onConsumeInitialStudioMode: () -> Unit = {},
    onSubmitPost: (
        text: String,
        type: String,
        mediaUri: String?,
        previewUri: String?,
        editDraft: MediaEditDraft
    ) -> Unit,
    onRetryUpload: () -> Unit,
    onDismissUpload: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val studioStrings = strings.mediaStudio
    val aiLang = AppStringsRegistry.aiLanguageName(selectedLanguageCode)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var postText by remember { mutableStateOf(initialText) }
    var selectedType by remember { mutableStateOf("text") } // "text", "camera", "gallery", "reels", "live"
    var selectedMediaUri by remember { mutableStateOf<String?>(null) }
    var selectedPreviewUri by remember { mutableStateOf<String?>(null) }
    var selectedIsVideo by remember { mutableStateOf(false) }

    var editDraft by remember { mutableStateOf(MediaEditDraft()) }
    var showEditedModePanel by remember { mutableStateOf(false) }
    var studioDialogMode by remember { mutableStateOf<String?>(null) } // "record" or "live"
    var transitionTriggerKey by remember { mutableIntStateOf(0) }

    var showAiCaptionDialog by remember { mutableStateOf(false) }
    var aiTopicInput by remember { mutableStateOf("") }
    var aiSelectedTone by remember(strings.aiTones) {
        mutableStateOf(strings.aiTones.firstOrNull() ?: "Engaging")
    }
    var isGeneratingCaption by remember { mutableStateOf(false) }
    var generatedCaptionResult by remember { mutableStateOf("") }
    var isEnhancingText by remember { mutableStateOf(false) }

    val isUploading = uploadState is PostUploadState.Uploading

    LaunchedEffect(initialText) {
        if (initialText.isNotBlank()) {
            postText = initialText
        }
    }

    // Clear draft fields once upload completes successfully
    LaunchedEffect(uploadState) {
        if (uploadState is PostUploadState.Success) {
            postText = ""
            selectedMediaUri = null
            selectedPreviewUri = null
            selectedIsVideo = false
            selectedType = "text"
            editDraft = MediaEditDraft()
            showEditedModePanel = false
        }
    }

    // Generate video preview thumbnail when a video is picked
    LaunchedEffect(selectedMediaUri) {
        val uriStr = selectedMediaUri
        if (uriStr != null) {
            val isVid = MediaProcessor.isVideoUri(context, uriStr, selectedType)
            selectedIsVideo = isVid
            selectedPreviewUri = if (isVid) {
                MediaProcessor.extractVideoThumbnailPreview(context, uriStr) ?: uriStr
            } else {
                uriStr
            }
        } else {
            selectedPreviewUri = null
            selectedIsVideo = selectedType == "reels" || selectedType == "live"
        }
    }

    fun saveBitmapToCache(bitmap: Bitmap): String? {
        return try {
            val file = File(context.cacheDir, "camera_capture_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, outputStream)
                outputStream.flush()
            }
            Uri.fromFile(file).toString()
        } catch (_: Exception) {
            null
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val uriStr = uri.toString()
            val isVid = MediaProcessor.isVideoUri(context, uriStr)
            selectedMediaUri = uriStr
            selectedIsVideo = isVid
            if (isVid) {
                selectedType = "reels"
                showEditedModePanel = true
            } else if (selectedType != "reels") {
                selectedType = "gallery"
            }
            onShowSnackbar(if (isVid) strings.mediaVideoSelected else strings.mediaGallerySelected)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val savedUri = saveBitmapToCache(bitmap)
            selectedMediaUri = savedUri
            selectedPreviewUri = savedUri
            selectedIsVideo = false
            selectedType = "camera"
            onShowSnackbar(strings.mediaCameraSelected)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraLauncher.launch(null)
        } else {
            onShowSnackbar(strings.cameraPermissionRequired)
        }
    }

    LaunchedEffect(initialStudioMode) {
        when (initialStudioMode) {
            "gallery" -> {
                onConsumeInitialStudioMode()
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                )
            }
            "reels" -> {
                onConsumeInitialStudioMode()
                selectedType = "reels"
                selectedIsVideo = true
                showEditedModePanel = true
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                )
            }
            "record" -> {
                onConsumeInitialStudioMode()
                studioDialogMode = "record"
            }
            "live" -> {
                onConsumeInitialStudioMode()
                studioDialogMode = "live"
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("create_post_screen")
    ) {
        if (uploadState !is PostUploadState.Idle) {
            UploadProgressCard(
                uploadState = uploadState,
                onRetry = onRetryUpload,
                onDismiss = onDismissUpload,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.createPostTitle,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            )

            if ((selectedMediaUri != null || selectedType != "text" || editDraft != MediaEditDraft()) && !isUploading) {
                TextButton(
                    onClick = {
                        selectedMediaUri = null
                        selectedPreviewUri = null
                        selectedIsVideo = false
                        selectedType = "text"
                        editDraft = MediaEditDraft()
                        showEditedModePanel = false
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = strings.removeLabel,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = strings.removeLabel,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // AI Assistant Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showAiCaptionDialog = true },
                enabled = !isUploading,
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("open_ai_caption_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = BrandPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.aiCaptionBtn, color = BrandPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    if (postText.isBlank()) {
                        onShowSnackbar(strings.enterTextFirstForAi)
                    } else {
                        isEnhancingText = true
                        coroutineScope.launch {
                            try {
                                val improved = aiRepository.enhancePostText(postText, languageName = aiLang)
                                postText = improved
                                onShowSnackbar(strings.aiEnhanceSuccess)
                            } catch (_: Exception) {
                                onShowSnackbar(strings.aiEnhanceFailed)
                            } finally {
                                isEnhancingText = false
                            }
                        }
                    }
                },
                enabled = !isEnhancingText && !isUploading,
                colors = ButtonDefaults.buttonColors(containerColor = BrandAccent.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("ai_enhance_post_button")
            ) {
                if (isEnhancingText) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = BrandAccent)
                } else {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = BrandAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.aiEnhanceBtn, color = BrandAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Caption Input Field
        OutlinedTextField(
            value = postText,
            onValueChange = { postText = it },
            enabled = !isUploading,
            placeholder = {
                Text(
                    text = if (selectedType == "reels" || selectedType == "live") strings.reelsTextPlaceholder else strings.postTextPlaceholder,
                    fontSize = 14.sp
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .testTag("create_post_text_field"),
            shape = RoundedCornerShape(12.dp)
        )

        // Selected Media / Reels / Live Preview Box with Filter, Transition & Moji Stickers
        if (selectedMediaUri != null) {
            Spacer(modifier = Modifier.height(14.dp))
            val activeFilter = ColorFilterMode.fromId(editDraft.colorFilter)
            val activeTransition = TransitionEffect.fromId(editDraft.transitionEffect)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1E2C))
                    .border(1.5.dp, BrandPrimary, RoundedCornerShape(12.dp))
            ) {
                TransitionEffectPreviewContainer(
                    effect = activeTransition,
                    triggerKey = transitionTriggerKey,
                    modifier = Modifier.fillMaxSize()
                ) {
                    AsyncImage(
                        model = selectedPreviewUri ?: selectedMediaUri,
                        contentDescription = "Selected Media Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        colorFilter = activeFilter.colorFilter()
                    )

                    if (selectedIsVideo) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayCircleFilled,
                                contentDescription = strings.videoAttachedBadge,
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    AppliedMediaDecorationsOverlay(
                        colorFilterId = editDraft.colorFilter,
                        transitionEffectId = editDraft.transitionEffect,
                        mojiStickers = editDraft.mojiStickers,
                        isLiveStream = editDraft.isLiveStream,
                        playbackSpeed = editDraft.playbackSpeed,
                        isMuted = editDraft.isMuted,
                        languageCode = selectedLanguageCode
                    )
                }

                // Attachment type badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (selectedIsVideo) Icons.Default.Videocam else Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (selectedIsVideo) strings.videoAttachedBadge else strings.photoAttachedBadge,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!isUploading) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                            .clickable {
                                selectedMediaUri = null
                                selectedPreviewUri = null
                                selectedIsVideo = false
                                selectedType = "text"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = strings.removeLabel,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        } else if (selectedType == "reels" || selectedType == "live") {
            Spacer(modifier = Modifier.height(14.dp))
            val activeTransition = TransitionEffect.fromId(editDraft.transitionEffect)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(155.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF1F1D36), BrandPrimary)
                        )
                    )
                    .border(1.5.dp, BrandPrimary, RoundedCornerShape(12.dp))
            ) {
                TransitionEffectPreviewContainer(
                    effect = activeTransition,
                    triggerKey = transitionTriggerKey,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (selectedType == "live") Icons.Default.LiveTv else Icons.Default.VideoCall,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(42.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (selectedType == "live") studioStrings.liveBadge else strings.reelsModeSelectedTitle,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }
                    AppliedMediaDecorationsOverlay(
                        colorFilterId = editDraft.colorFilter,
                        transitionEffectId = editDraft.transitionEffect,
                        mojiStickers = editDraft.mojiStickers,
                        isLiveStream = editDraft.isLiveStream || selectedType == "live",
                        playbackSpeed = editDraft.playbackSpeed,
                        isMuted = editDraft.isMuted,
                        languageCode = selectedLanguageCode
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.selectMediaLabel,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            )

            FilterChip(
                selected = showEditedModePanel,
                onClick = { showEditedModePanel = !showEditedModePanel },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.MovieFilter,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = {
                    Text(
                        text = studioStrings.editedModeBtn,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BrandPrimary.copy(alpha = 0.16f),
                    selectedLabelColor = BrandPrimary
                ),
                modifier = Modifier.testTag("toggle_edited_mode_button")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 1: Camera, Gallery, Reels/Video
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val hasCam = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasCam) {
                        cameraLauncher.launch(null)
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                enabled = !isUploading,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
                    .testTag("pick_camera_button"),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (selectedType == "camera") Color(0xFFE91E63) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = strings.cameraLabel,
                        tint = Color(0xFFE91E63),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(strings.cameraLabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            OutlinedButton(
                onClick = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                },
                enabled = !isUploading,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
                    .testTag("pick_gallery_button"),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (selectedType == "gallery") Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = strings.galleryLabel,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(strings.galleryLabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            OutlinedButton(
                onClick = {
                    selectedType = "reels"
                    selectedIsVideo = true
                    showEditedModePanel = true
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                    onShowSnackbar(strings.reelsModeSelectedSnackbar)
                },
                enabled = !isUploading,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
                    .testTag("pick_reels_button"),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (selectedType == "reels") BrandPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.VideoCall,
                        contentDescription = strings.reelsLabel,
                        tint = BrandPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(strings.reelsLabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 2: Recording Mode & Go Live
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { studioDialogMode = "record" },
                enabled = !isUploading,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp)
                    .testTag("pick_record_button"),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.FiberManualRecord,
                    contentDescription = studioStrings.recordingModeBtn,
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = studioStrings.recordingModeBtn,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Button(
                onClick = { studioDialogMode = "live" },
                enabled = !isUploading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp)
                    .testTag("pick_live_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LiveTv,
                    contentDescription = studioStrings.goLiveBtn,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = studioStrings.goLiveBtn,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Expanded Edited Mode Panel (Speed, Mute, Trim, Color Filter, Transition, Moji Stickers)
        if (showEditedModePanel) {
            Spacer(modifier = Modifier.height(14.dp))
            MediaEditControlsPanel(
                draft = editDraft,
                isVideo = selectedIsVideo || selectedType == "reels" || selectedType == "live",
                selectedLanguageCode = selectedLanguageCode,
                onDraftChange = { editDraft = it },
                onPreviewTransition = { transitionTriggerKey++ }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Publish Button
        Button(
            onClick = {
                if (postText.isNotBlank() || selectedMediaUri != null || selectedType != "text") {
                    val finalCaption = when {
                        postText.isNotBlank() -> postText.trim()
                        selectedType == "live" || editDraft.isLiveStream -> studioStrings.defaultLivePostText
                        selectedIsVideo || selectedType == "reels" -> studioStrings.defaultReelPostText
                        else -> strings.defaultMediaPostText
                    }
                    onSubmitPost(finalCaption, selectedType, selectedMediaUri, selectedPreviewUri, editDraft)
                } else {
                    onShowSnackbar(strings.enterTextOrMediaSnackbar)
                }
            },
            enabled = !isUploading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("publish_post_button"),
            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
            shape = RoundedCornerShape(10.dp)
        ) {
            if (isUploading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.publishingButton,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.publishButton,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Live Broadcast & Video Recording Studio Dialog
    val activeStudioMode = studioDialogMode
    if (activeStudioMode != null) {
        LiveAndRecordStudioDialog(
            initialMode = activeStudioMode,
            selectedLanguageCode = selectedLanguageCode,
            onDismiss = { studioDialogMode = null },
            onRecordedVideoReady = { videoUri, recordedDraft ->
                studioDialogMode = null
                selectedType = "reels"
                selectedIsVideo = true
                selectedMediaUri = videoUri
                editDraft = recordedDraft
                showEditedModePanel = true
            },
            onPublishLivePost = { title, thumbnailUri, liveDraft ->
                studioDialogMode = null
                onShowSnackbar(studioStrings.livePostedSnackbar)
                onSubmitPost(title, "live", thumbnailUri, thumbnailUri, liveDraft.copy(isLiveStream = true))
            },
            onShowSnackbar = onShowSnackbar
        )
    }

    // AI Caption Generator Dialog
    if (showAiCaptionDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isGeneratingCaption) showAiCaptionDialog = false
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = BrandPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.aiCaptionDialogTitle, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            },
            text = {
                Column {
                    Text(strings.aiTopicPrompt, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = aiTopicInput,
                        onValueChange = { aiTopicInput = it },
                        placeholder = { Text(strings.aiTopicPlaceholder, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(strings.aiToneLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        strings.aiTones.take(3).forEach { tone ->
                            AssistChip(
                                onClick = { aiSelectedTone = tone },
                                label = { Text(tone, fontSize = 11.sp) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (aiSelectedTone == tone) BrandPrimary.copy(alpha = 0.15f) else Color.Transparent,
                                    labelColor = if (aiSelectedTone == tone) BrandPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }

                    if (generatedCaptionResult.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(strings.aiGeneratedCaptionLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(10.dp)
                        ) {
                            Text(generatedCaptionResult, fontSize = 13.sp, lineHeight = 18.sp)
                        }
                    }
                }
            },
            confirmButton = {
                if (generatedCaptionResult.isNotBlank()) {
                    Button(
                        onClick = {
                            postText = generatedCaptionResult
                            showAiCaptionDialog = false
                            generatedCaptionResult = ""
                            aiTopicInput = ""
                            onShowSnackbar(strings.aiCaptionUsedSnackbar)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                    ) {
                        Text(strings.aiUseCaptionBtn)
                    }
                } else {
                    Button(
                        onClick = {
                            if (aiTopicInput.isBlank()) {
                                onShowSnackbar(strings.aiEnterTopicSnackbar)
                            } else {
                                isGeneratingCaption = true
                                coroutineScope.launch {
                                    try {
                                        val result = aiRepository.generateCaption(
                                            topic = aiTopicInput,
                                            tone = aiSelectedTone,
                                            languageName = aiLang
                                        )
                                        generatedCaptionResult = result
                                    } catch (_: Exception) {
                                        onShowSnackbar(strings.aiCaptionFailedSnackbar)
                                    } finally {
                                        isGeneratingCaption = false
                                    }
                                }
                            }
                        },
                        enabled = !isGeneratingCaption && aiTopicInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                    ) {
                        if (isGeneratingCaption) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text(strings.aiGenerateBtn)
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAiCaptionDialog = false
                        generatedCaptionResult = ""
                    }
                ) {
                    Text(strings.close)
                }
            }
        )
    }
}
