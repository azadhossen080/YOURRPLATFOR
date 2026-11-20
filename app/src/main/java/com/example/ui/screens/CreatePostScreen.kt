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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.AiRepository
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
    onSubmitPost: (text: String, type: String, mediaUri: String?, previewUri: String?) -> Unit,
    onRetryUpload: () -> Unit,
    onDismissUpload: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val aiLang = AppStringsRegistry.aiLanguageName(selectedLanguageCode)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var postText by remember { mutableStateOf(initialText) }
    var selectedType by remember { mutableStateOf("text") } // "text", "camera", "gallery", "reels"
    var selectedMediaUri by remember { mutableStateOf<String?>(null) }
    var selectedPreviewUri by remember { mutableStateOf<String?>(null) }
    var selectedIsVideo by remember { mutableStateOf(false) }

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
            selectedIsVideo = false
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
            if (selectedType != "reels") {
                selectedType = if (isVid) "reels" else "gallery"
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("create_post_screen")
    ) {
        // Active Upload / Processing Card
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

            if ((selectedMediaUri != null || selectedType != "text") && !isUploading) {
                TextButton(
                    onClick = {
                        selectedMediaUri = null
                        selectedPreviewUri = null
                        selectedIsVideo = false
                        selectedType = "text"
                    }
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
                    text = if (selectedType == "reels") strings.reelsTextPlaceholder else strings.postTextPlaceholder,
                    fontSize = 14.sp
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .testTag("create_post_text_field"),
            shape = RoundedCornerShape(12.dp)
        )

        // Selected Media Preview Box
        if (selectedMediaUri != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1E2C))
                    .border(1.5.dp, BrandPrimary, RoundedCornerShape(12.dp))
            ) {
                AsyncImage(
                    model = selectedPreviewUri ?: selectedMediaUri,
                    contentDescription = "Selected Media Preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                if (selectedIsVideo) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.25f)),
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
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
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
        } else if (selectedType == "reels") {
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BrandPrimary.copy(alpha = 0.1f))
                    .border(1.5.dp, BrandPrimary, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.VideoCall,
                        contentDescription = null,
                        tint = BrandPrimary,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.reelsModeSelectedTitle,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BrandPrimary
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = strings.selectMediaLabel,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Media Selection Options: Camera, Gallery, Reels
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
                modifier = Modifier
                    .weight(1f)
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
                    Text(strings.cameraLabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            OutlinedButton(
                onClick = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                },
                enabled = !isUploading,
                modifier = Modifier
                    .weight(1f)
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
                    Text(strings.galleryLabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            OutlinedButton(
                onClick = {
                    selectedType = "reels"
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                    onShowSnackbar(strings.reelsModeSelectedSnackbar)
                },
                enabled = !isUploading,
                modifier = Modifier
                    .weight(1f)
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
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(strings.reelsLabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Publish Button
        Button(
            onClick = {
                if (postText.isNotBlank() || selectedMediaUri != null || selectedType != "text") {
                    val finalCaption = if (postText.isBlank()) strings.defaultMediaPostText else postText.trim()
                    onSubmitPost(finalCaption, selectedType, selectedMediaUri, selectedPreviewUri)
                } else {
                    onShowSnackbar(strings.enterTextOrMediaSnackbar)
                }
            },
            enabled = !isUploading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
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
