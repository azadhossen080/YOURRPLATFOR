package com.example.ui.components

import android.net.Uri
import android.os.Build
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.model.MediaEditDraft
import com.example.model.OVERLAY_TEXT_COLORS
import com.example.model.SOUND_TRACK_PRESETS
import com.example.model.TransitionEffect
import com.example.ui.theme.BrandAccent
import com.example.ui.theme.BrandPrimary
import com.example.util.LocalAppStrings
import kotlin.math.roundToInt

enum class VideoStudioTool {
    NONE,
    SOUND,
    FILTER,
    TRIM,
    SLOW_MOTION,
    TIMER,
    TEXT,
    COLOR,
    MOJI
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoStudioPreviewEditor(
    mediaUri: String?,
    previewUri: String?,
    draft: MediaEditDraft,
    selectedLanguageCode: String,
    onDraftChange: (MediaEditDraft) -> Unit,
    onOpenCameraCapture: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val studioStrings = strings.mediaStudio

    var activeTool by remember { mutableStateOf(VideoStudioTool.NONE) }
    var transitionTriggerKey by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(true) }

    val filterMode = ColorFilterMode.fromId(draft.colorFilter)
    val transitionMode = TransitionEffect.fromId(draft.transitionEffect)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0E101C))
            .testTag("video_preview_studio")
    ) {
        // 1. Live Video / Thumbnail Preview Canvas
        TransitionEffectPreviewContainer(
            effect = transitionMode,
            triggerKey = transitionTriggerKey,
            modifier = Modifier
                .fillMaxSize()
                .clickable { isPlaying = !isPlaying }
        ) {
            if (!mediaUri.isNullOrBlank()) {
                VideoStudioSurfacePlayer(
                    videoUri = mediaUri,
                    thumbnailUri = previewUri,
                    colorFilterMode = filterMode,
                    shouldPlay = isPlaying,
                    isMuted = draft.isMuted,
                    playbackSpeed = draft.playbackSpeed,
                    trimStartFraction = draft.trimStartFraction
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1A1C33),
                                    BrandPrimary.copy(alpha = 0.72f),
                                    Color(0xFF121320)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (!previewUri.isNullOrBlank()) {
                        AsyncImage(
                            model = previewUri,
                            contentDescription = "Video preview frame",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            colorFilter = filterMode.colorFilter()
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.PlayCircleFilled,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = strings.reelsModeSelectedTitle,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = if (draft.useFrontCamera) "Front Lens" else "Back Lens",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            AppliedMediaDecorationsOverlay(
                colorFilterId = draft.colorFilter,
                transitionEffectId = draft.transitionEffect,
                mojiStickers = draft.mojiStickers,
                isLiveStream = draft.isLiveStream,
                playbackSpeed = draft.playbackSpeed,
                isMuted = draft.isMuted,
                overlayText = draft.overlayText,
                overlayColorHex = draft.overlayColorHex,
                soundName = draft.soundName,
                timerDurationSec = draft.timerDurationSec,
                languageCode = selectedLanguageCode
            )
        }

        // 2. Top Controls: Close (Left), Add Sound (Top Middle), Next (Top Right)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                    )
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .testTag("video_preview_back_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = strings.back,
                    tint = Color.White
                )
            }

            // Top Middle: Add Sound Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.Center)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        if (activeTool == VideoStudioTool.SOUND) BrandPrimary else Color.Black.copy(alpha = 0.65f)
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .clickable {
                        activeTool = if (activeTool == VideoStudioTool.SOUND) VideoStudioTool.NONE else VideoStudioTool.SOUND
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("video_top_add_sound_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Audiotrack,
                    contentDescription = studioStrings.addSoundBtn,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (draft.soundName.isNotBlank() && draft.soundName != "Original Audio") {
                        draft.soundName
                    } else {
                        studioStrings.addSoundBtn
                    },
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Top Right: Next Button
            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                shape = RoundedCornerShape(22.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .heightIn(min = 48.dp)
                    .testTag("video_preview_next_button")
            ) {
                Text(
                    text = studioStrings.nextStepBtn,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = studioStrings.nextStepBtn,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // 3. Left Side Vertical Tool Rail:
        //    Filter, Trims, Front/Back Camera, Slow Motion, Timer, Text, Color, Moji
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 10.dp, top = 66.dp, bottom = if (activeTool != VideoStudioTool.NONE) 180.dp else 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LeftStudioToolButton(
                icon = Icons.Filled.FilterVintage,
                label = studioStrings.filterToolLabel,
                selected = activeTool == VideoStudioTool.FILTER || draft.colorFilter != ColorFilterMode.NONE.id,
                onClick = {
                    activeTool = if (activeTool == VideoStudioTool.FILTER) VideoStudioTool.NONE else VideoStudioTool.FILTER
                },
                testTag = "left_rail_filter"
            )

            LeftStudioToolButton(
                icon = Icons.Filled.ContentCut,
                label = studioStrings.trimToolLabel,
                selected = activeTool == VideoStudioTool.TRIM || draft.trimStartFraction > 0f || draft.trimEndFraction < 1f,
                onClick = {
                    activeTool = if (activeTool == VideoStudioTool.TRIM) VideoStudioTool.NONE else VideoStudioTool.TRIM
                },
                testTag = "left_rail_trim"
            )

            LeftStudioToolButton(
                icon = Icons.Filled.Cameraswitch,
                label = studioStrings.flipToolLabel,
                selected = !draft.useFrontCamera,
                onClick = {
                    onDraftChange(draft.copy(useFrontCamera = !draft.useFrontCamera))
                    if (mediaUri.isNullOrBlank()) {
                        onOpenCameraCapture()
                    }
                },
                testTag = "left_rail_camera_flip"
            )

            LeftStudioToolButton(
                icon = Icons.Filled.SlowMotionVideo,
                label = studioStrings.slowMoToolLabel,
                selected = activeTool == VideoStudioTool.SLOW_MOTION || draft.playbackSpeed != 1.0f,
                onClick = {
                    activeTool = if (activeTool == VideoStudioTool.SLOW_MOTION) VideoStudioTool.NONE else VideoStudioTool.SLOW_MOTION
                },
                testTag = "left_rail_slow_motion"
            )

            LeftStudioToolButton(
                icon = Icons.Filled.Timer,
                label = studioStrings.timerToolLabel,
                selected = activeTool == VideoStudioTool.TIMER || draft.timerDurationSec > 0,
                onClick = {
                    activeTool = if (activeTool == VideoStudioTool.TIMER) VideoStudioTool.NONE else VideoStudioTool.TIMER
                },
                testTag = "left_rail_timer"
            )

            LeftStudioToolButton(
                icon = Icons.Filled.Title,
                label = studioStrings.textToolLabel,
                selected = activeTool == VideoStudioTool.TEXT || draft.overlayText.isNotBlank(),
                onClick = {
                    activeTool = if (activeTool == VideoStudioTool.TEXT) VideoStudioTool.NONE else VideoStudioTool.TEXT
                },
                testTag = "left_rail_text"
            )

            LeftStudioToolButton(
                icon = Icons.Filled.Palette,
                label = studioStrings.colorToolLabel,
                selected = activeTool == VideoStudioTool.COLOR || draft.transitionEffect != TransitionEffect.NONE.id,
                onClick = {
                    activeTool = if (activeTool == VideoStudioTool.COLOR) VideoStudioTool.NONE else VideoStudioTool.COLOR
                },
                testTag = "left_rail_color"
            )

            LeftStudioToolButton(
                icon = Icons.Filled.Mood,
                label = studioStrings.mojiToolLabel,
                selected = activeTool == VideoStudioTool.MOJI || draft.mojiStickers.isNotEmpty(),
                onClick = {
                    activeTool = if (activeTool == VideoStudioTool.MOJI) VideoStudioTool.NONE else VideoStudioTool.MOJI
                },
                testTag = "left_rail_moji"
            )
        }

        // 4. Contextual Bottom Tool Drawer
        AnimatedVisibility(
            visible = activeTool != VideoStudioTool.NONE,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Surface(
                color = Color(0xFF141626).copy(alpha = 0.94f),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("video_tool_drawer")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val toolTitle = when (activeTool) {
                            VideoStudioTool.SOUND -> studioStrings.soundPickerTitle
                            VideoStudioTool.FILTER -> studioStrings.colorModeFilterLabel
                            VideoStudioTool.TRIM -> studioStrings.trimRangeLabel
                            VideoStudioTool.SLOW_MOTION -> studioStrings.speedLabel
                            VideoStudioTool.TIMER -> studioStrings.recordingDurationLabel
                            VideoStudioTool.TEXT -> studioStrings.textToolLabel
                            VideoStudioTool.COLOR -> "${studioStrings.colorToolLabel} & ${studioStrings.transitionLabel}"
                            VideoStudioTool.MOJI -> studioStrings.liveMojiStickerLabel
                            VideoStudioTool.NONE -> ""
                        }
                        Text(
                            text = toolTitle,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        IconButton(
                            onClick = { activeTool = VideoStudioTool.NONE },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = strings.close,
                                tint = Color(0xFF69F0AE)
                            )
                        }
                    }

                    when (activeTool) {
                        VideoStudioTool.SOUND -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                FilterChip(
                                    selected = draft.isMuted,
                                    onClick = { onDraftChange(draft.copy(isMuted = !draft.isMuted)) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (draft.isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    label = {
                                        Text(if (draft.isMuted) studioStrings.muteAudioLabel else studioStrings.unmuteAudioLabel)
                                    },
                                    modifier = Modifier.testTag("studio_sound_mute_chip")
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SOUND_TRACK_PRESETS.forEach { track ->
                                    val selected = draft.soundName == track
                                    FilterChip(
                                        selected = selected,
                                        onClick = { onDraftChange(draft.copy(soundName = track, isMuted = false)) },
                                        label = { Text(track, fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = BrandPrimary,
                                            selectedLabelColor = Color.White
                                        ),
                                        modifier = Modifier.testTag("sound_track_${track.hashCode()}")
                                    )
                                }
                            }
                        }

                        VideoStudioTool.FILTER -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ColorFilterMode.entries.forEach { mode ->
                                    val selected = draft.colorFilter == mode.id
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(
                                                width = if (selected) 2.dp else 1.dp,
                                                color = if (selected) BrandPrimary else Color.White.copy(alpha = 0.25f),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .background(if (selected) BrandPrimary.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.4f))
                                            .clickable { onDraftChange(draft.copy(colorFilter = mode.id)) }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                            .testTag("video_filter_${mode.id}")
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Color(mode.swatchHex))
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = mode.localizedLabel(selectedLanguageCode),
                                            color = Color.White,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }

                        VideoStudioTool.TRIM -> {
                            Text(
                                text = "${(draft.trimStartFraction * 100).roundToInt()}% - ${(draft.trimEndFraction * 100).roundToInt()}%",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp
                            )
                            RangeSlider(
                                value = draft.trimStartFraction..draft.trimEndFraction,
                                onValueChange = { range ->
                                    val start = range.start.coerceIn(0f, 0.9f)
                                    val end = range.endInclusive.coerceAtLeast(start + 0.1f).coerceIn(0.1f, 1f)
                                    onDraftChange(draft.copy(trimStartFraction = start, trimEndFraction = end))
                                },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(thumbColor = BrandPrimary, activeTrackColor = BrandPrimary),
                                modifier = Modifier.testTag("video_trim_slider")
                            )
                        }

                        VideoStudioTool.SLOW_MOTION -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(0.5f, 1.0f, 1.5f, 2.0f).forEach { spd ->
                                    val selected = draft.playbackSpeed == spd
                                    FilterChip(
                                        selected = selected,
                                        onClick = { onDraftChange(draft.copy(playbackSpeed = spd)) },
                                        label = { Text("${spd}x", fontWeight = FontWeight.Bold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = BrandPrimary,
                                            selectedLabelColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("video_speed_${spd}")
                                    )
                                }
                            }
                        }

                        VideoStudioTool.TIMER -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(0, 15, 30, 60).forEach { sec ->
                                    val selected = draft.timerDurationSec == sec
                                    FilterChip(
                                        selected = selected,
                                        onClick = { onDraftChange(draft.copy(timerDurationSec = sec)) },
                                        label = {
                                            Text(if (sec == 0) "Off" else "${sec}s", fontWeight = FontWeight.Bold)
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = BrandPrimary,
                                            selectedLabelColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("video_timer_${sec}")
                                    )
                                }
                            }
                        }

                        VideoStudioTool.TEXT -> {
                            OutlinedTextField(
                                value = draft.overlayText,
                                onValueChange = { onDraftChange(draft.copy(overlayText = it)) },
                                placeholder = {
                                    Text(studioStrings.overlayTextPlaceholder, color = Color.White.copy(alpha = 0.6f))
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = BrandPrimary,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("video_overlay_text_input"),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        VideoStudioTool.COLOR -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OVERLAY_TEXT_COLORS.forEach { hex ->
                                    val isSelected = draft.overlayColorHex == hex
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(hex))
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) Color.White else Color.Gray,
                                                shape = CircleShape
                                            )
                                            .clickable { onDraftChange(draft.copy(overlayColorHex = hex)) }
                                            .testTag("overlay_color_${hex}")
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                TransitionEffect.entries.forEach { effect ->
                                    val selected = draft.transitionEffect == effect.id
                                    FilterChip(
                                        selected = selected,
                                        onClick = {
                                            onDraftChange(draft.copy(transitionEffect = effect.id))
                                            transitionTriggerKey++
                                        },
                                        label = { Text(effect.localizedLabel(selectedLanguageCode), fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = BrandAccent,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }

                        VideoStudioTool.MOJI -> {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                DEFAULT_MOJI_STICKERS.forEach { emoji ->
                                    val selected = emoji in draft.mojiStickers
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (selected) BrandPrimary.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.5f))
                                            .border(
                                                width = if (selected) 2.dp else 1.dp,
                                                color = if (selected) Color.White else Color.White.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                val updated = if (selected) draft.mojiStickers - emoji else (draft.mojiStickers + emoji).take(6)
                                                onDraftChange(draft.copy(mojiStickers = updated))
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = emoji, fontSize = 20.sp)
                                    }
                                }
                            }
                        }

                        VideoStudioTool.NONE -> Unit
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = onOpenCameraCapture,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.Cameraswitch, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(studioStrings.captureVideoCameraBtn, color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeftStudioToolButton(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .width(54.dp)
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) BrandPrimary.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun VideoStudioSurfacePlayer(
    videoUri: String,
    thumbnailUri: String?,
    colorFilterMode: ColorFilterMode,
    shouldPlay: Boolean,
    isMuted: Boolean,
    playbackSpeed: Float,
    trimStartFraction: Float
) {
    var videoViewRef by remember(videoUri) { mutableStateOf<VideoView?>(null) }
    var mediaPlayerRef by remember(videoUri) { mutableStateOf<android.media.MediaPlayer?>(null) }
    var prepared by remember(videoUri) { mutableStateOf(false) }

    LaunchedEffect(shouldPlay, isMuted, playbackSpeed, prepared) {
        val mp = mediaPlayerRef
        val vv = videoViewRef
        if (prepared && mp != null && vv != null) {
            try {
                val vol = if (isMuted) 0f else 1f
                mp.setVolume(vol, vol)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    mp.playbackParams = mp.playbackParams.setSpeed(playbackSpeed.coerceIn(0.5f, 2.0f))
                }
                if (shouldPlay && !vv.isPlaying) vv.start() else if (!shouldPlay && vv.isPlaying) vv.pause()
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
                        prepared = true
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
