package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.model.ColorFilterMode
import com.example.model.DEFAULT_MOJI_STICKERS
import com.example.model.LiveFloatingMoji
import com.example.model.MediaEditDraft
import com.example.model.TransitionEffect
import com.example.ui.theme.BrandAccent
import com.example.ui.theme.BrandPrimary
import com.example.util.LocalAppStrings
import kotlinx.coroutines.delay
import java.io.File
import java.util.UUID
import kotlin.math.roundToInt
import kotlin.random.Random

fun ColorFilterMode.localizedLabel(languageCode: String): String {
    return if (languageCode.startsWith("bn", ignoreCase = true)) labelBn else labelEn
}

fun TransitionEffect.localizedLabel(languageCode: String): String {
    return if (languageCode.startsWith("bn", ignoreCase = true)) labelBn else labelEn
}

@Composable
fun TransitionEffectPreviewContainer(
    effect: TransitionEffect,
    triggerKey: Int,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val progress = remember { Animatable(1f) }

    LaunchedEffect(effect, triggerKey) {
        if (effect == TransitionEffect.NONE) {
            progress.snapTo(1f)
        } else {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing)
            )
        }
    }

    val t = progress.value
    val alphaVal = when (effect) {
        TransitionEffect.FADE -> t
        TransitionEffect.FLASH -> if (t < 0.4f) 1f else 0.4f + (t - 0.4f)
        else -> 1f
    }
    val scaleVal = when (effect) {
        TransitionEffect.ZOOM_IN -> 0.84f + 0.16f * t
        else -> 1f
    }
    val offsetYPx = when (effect) {
        TransitionEffect.SLIDE_UP -> ((1f - t) * 70f).roundToInt()
        else -> 0
    }
    val rotationDeg = when (effect) {
        TransitionEffect.SPIN -> (1f - t) * 14f
        else -> 0f
    }

    Box(
        modifier = modifier
            .alpha(alphaVal)
            .scale(scaleVal)
            .rotate(rotationDeg)
            .offset { IntOffset(0, offsetYPx) }
    ) {
        content()
        if (effect == TransitionEffect.FLASH && t < 0.45f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = (0.45f - t).coerceIn(0f, 0.45f)))
            )
        }
    }
}

val VerifiedBlueColor = Color(0xFF1D9BF0)
val VipGoldStart = Color(0xFFFFB300)
val VipGoldEnd = Color(0xFFFF6F00)

@Composable
fun VerifiedAndVipBadges(
    isVerified: Boolean,
    isVip: Boolean,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val strings = LocalAppStrings.current
    if (!isVerified && !isVip) return
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (isVerified) {
            Icon(
                imageVector = Icons.Filled.Verified,
                contentDescription = strings.mediaStudio.verifiedBadgeDesc,
                tint = VerifiedBlueColor,
                modifier = Modifier
                    .size(if (compact) 14.dp else 17.dp)
                    .testTag("verified_blue_badge")
            )
        }
        if (isVip) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(VipGoldStart, VipGoldEnd)
                        )
                    )
                    .padding(horizontal = if (compact) 5.dp else 6.dp, vertical = 2.dp)
                    .testTag("vip_crown_badge")
            ) {
                Icon(
                    imageVector = Icons.Filled.WorkspacePremium,
                    contentDescription = strings.mediaStudio.vipBadgeLabel,
                    tint = Color.White,
                    modifier = Modifier.size(if (compact) 10.dp else 12.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = strings.mediaStudio.vipBadgeLabel,
                    color = Color.White,
                    fontSize = if (compact) 9.sp else 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun AppliedMediaDecorationsOverlay(
    colorFilterId: String,
    transitionEffectId: String,
    mojiStickers: List<String>,
    isLiveStream: Boolean,
    playbackSpeed: Float = 1f,
    isMuted: Boolean = false,
    overlayText: String = "",
    overlayColorHex: Long = 0xFFFFFFFF,
    soundName: String = "Original Audio",
    timerDurationSec: Int = 0,
    languageCode: String = "en_US",
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val filter = ColorFilterMode.fromId(colorFilterId)
    val transition = TransitionEffect.fromId(transitionEffectId)

    Box(modifier = modifier.fillMaxSize()) {
        if (filter != ColorFilterMode.NONE) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(filter.overlayColor())
            )
        }

        // Top-right Live & Effect Badges
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isLiveStream) {
                val pulse = rememberInfiniteTransition(label = "livePulse")
                val dotAlpha by pulse.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(700, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "liveDot"
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE53935))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .alpha(dotAlpha)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = strings.mediaStudio.liveBadge,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            if (filter != ColorFilterMode.NONE || transition != TransitionEffect.NONE || playbackSpeed != 1f || isMuted) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.58f))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    if (filter != ColorFilterMode.NONE) {
                        Text(
                            text = filter.localizedLabel(languageCode),
                            color = Color(0xFFFFD54F),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (transition != TransitionEffect.NONE) {
                        Text(
                            text = transition.localizedLabel(languageCode),
                            color = Color(0xFF80DEEA),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (playbackSpeed != 1f) {
                        Text(
                            text = "${playbackSpeed}x",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (isMuted) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = strings.mediaStudio.muteAudioLabel,
                            tint = Color(0xFFFF8A80),
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
        }

        if (overlayText.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.Black.copy(alpha = 0.52f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = overlayText,
                    color = Color(overlayColorHex),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // Bottom-left sound / timer strip & Moji Stickers Overlay
        if (soundName.isNotBlank() && soundName != "Original Audio" || timerDurationSec > 0) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.52f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (soundName.isNotBlank() && soundName != "Original Audio") {
                    Icon(
                        imageVector = Icons.Filled.Audiotrack,
                        contentDescription = null,
                        tint = Color(0xFF80DEEA),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = soundName,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (timerDurationSec > 0) {
                    Icon(
                        imageVector = Icons.Filled.Timer,
                        contentDescription = null,
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "${timerDurationSec}s",
                        color = Color(0xFFFFD54F),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (mojiStickers.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                mojiStickers.take(6).forEach { emoji ->
                    Text(text = emoji, fontSize = 20.sp)
                }
            }
        }
    }
}

@Composable
fun FloatingMojiStage(
    floatingMojis: List<LiveFloatingMoji>,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        floatingMojis.forEach { item ->
            SingleRisingMoji(
                item = item,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
}

@Composable
private fun SingleRisingMoji(
    item: LiveFloatingMoji,
    modifier: Modifier = Modifier
) {
    val anim = remember(item.id) { Animatable(0f) }
    LaunchedEffect(item.id) {
        anim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
        )
    }
    val progress = anim.value
    val yOffsetDp = (-40f - progress * 220f).roundToInt()
    val xOffsetDp = (24 + item.xFraction * 240).roundToInt()
    val alpha = (1f - progress * 0.9f).coerceIn(0f, 1f)
    val scale = 0.9f + 0.4f * (1f - kotlin.math.abs(progress - 0.5f) * 2f)

    Text(
        text = item.emoji,
        fontSize = 28.sp,
        modifier = modifier
            .offset { IntOffset(xOffsetDp, yOffsetDp) }
            .scale(scale)
            .alpha(alpha)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MediaEditControlsPanel(
    draft: MediaEditDraft,
    isVideo: Boolean,
    selectedLanguageCode: String,
    onDraftChange: (MediaEditDraft) -> Unit,
    onPreviewTransition: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val studioStrings = strings.mediaStudio

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("media_edit_controls_panel")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MovieFilter,
                        contentDescription = null,
                        tint = BrandPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = studioStrings.editedModeBtn,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                OutlinedButton(
                    onClick = onPreviewTransition,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("preview_transition_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(studioStrings.previewEffectsBtn, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Video Speed, Sound Mute, and Trim Range
            if (isVideo) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = studioStrings.speedLabel,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }

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
                                Text(
                                    text = if (draft.isMuted) studioStrings.muteAudioLabel else studioStrings.unmuteAudioLabel,
                                    fontSize = 11.sp
                                )
                            },
                            modifier = Modifier.testTag("toggle_mute_chip")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0.5f, 1.0f, 1.5f, 2.0f).forEach { spd ->
                            FilterChip(
                                selected = draft.playbackSpeed == spd,
                                onClick = { onDraftChange(draft.copy(playbackSpeed = spd)) },
                                label = { Text("${spd}x", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandPrimary.copy(alpha = 0.18f),
                                    selectedLabelColor = BrandPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("speed_chip_${spd}")
                            )
                        }
                    }

                    // Trim Slider
                    Text(
                        text = "${studioStrings.trimRangeLabel}: ${(draft.trimStartFraction * 100).roundToInt()}% - ${(draft.trimEndFraction * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        )
                    )
                    RangeSlider(
                        value = draft.trimStartFraction..draft.trimEndFraction,
                        onValueChange = { range ->
                            val start = range.start.coerceIn(0f, 0.9f)
                            val end = range.endInclusive.coerceAtLeast(start + 0.1f).coerceIn(0.1f, 1f)
                            onDraftChange(draft.copy(trimStartFraction = start, trimEndFraction = end))
                        },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = BrandPrimary,
                            activeTrackColor = BrandPrimary
                        ),
                        modifier = Modifier.testTag("trim_range_slider")
                    )
                }
            }

            // Color Mode Filter Picker
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FilterVintage, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = studioStrings.colorModeFilterLabel,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

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
                                    color = if (selected) BrandPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .background(
                                    if (selected) BrandPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                                )
                                .clickable { onDraftChange(draft.copy(colorFilter = mode.id)) }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                .testTag("color_filter_${mode.id}")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(Color(mode.swatchHex))
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = mode.localizedLabel(selectedLanguageCode),
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) BrandPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Transition Effect Picker
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = BrandAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = studioStrings.transitionLabel,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
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
                                onPreviewTransition()
                            },
                            label = {
                                Text(
                                    text = effect.localizedLabel(selectedLanguageCode),
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandAccent.copy(alpha = 0.18f),
                                selectedLabelColor = BrandAccent
                            ),
                            modifier = Modifier.testTag("transition_chip_${effect.id}")
                        )
                    }
                }
            }

            // Live Moji Stickers Picker
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Mood, contentDescription = null, tint = Color(0xFFFFA000), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = studioStrings.liveMojiStickerLabel,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

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
                                .background(
                                    if (selected) BrandPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                                )
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) BrandPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    val updated = if (selected) {
                                        draft.mojiStickers - emoji
                                    } else {
                                        (draft.mojiStickers + emoji).take(6)
                                    }
                                    onDraftChange(draft.copy(mojiStickers = updated))
                                }
                                .testTag("moji_sticker_${emoji.hashCode()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 20.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CameraPreviewSurface(
    useFrontCamera: Boolean,
    colorFilterMode: ColorFilterMode,
    hasCameraPermission: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var cameraAvailable by remember { mutableStateOf(true) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF141628), Color(0xFF282046), Color(0xFF121420))
                )
            )
    ) {
        if (hasCameraPermission && cameraAvailable) {
            val previewView = remember {
                PreviewView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            }

            DisposableEffect(useFrontCamera, lifecycleOwner) {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                val executor = ContextCompat.getMainExecutor(context)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val desiredSelector = if (useFrontCamera) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }
                        val selector = if (cameraProvider.hasCamera(desiredSelector)) {
                            desiredSelector
                        } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            null
                        }

                        cameraProvider.unbindAll()
                        if (selector != null) {
                            cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview)
                            cameraAvailable = true
                        } else {
                            cameraAvailable = false
                        }
                    } catch (_: Exception) {
                        cameraAvailable = false
                    }
                }, executor)

                onDispose {
                    try {
                        cameraProviderFuture.get().unbindAll()
                    } catch (_: Exception) {
                    }
                }
            }

            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (hasCameraPermission) "YOURPLATFORM Studio Lens" else LocalAppStrings.current.cameraPermissionRequired,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (colorFilterMode != ColorFilterMode.NONE) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorFilterMode.overlayColor())
            )
        }
    }
}

@Composable
fun LiveAndRecordStudioDialog(
    initialMode: String, // "live" or "record"
    selectedLanguageCode: String,
    onDismiss: () -> Unit,
    onRecordedVideoReady: (videoUri: String?, draft: MediaEditDraft) -> Unit,
    onPublishLivePost: (title: String, thumbnailUri: String?, draft: MediaEditDraft) -> Unit,
    onShowSnackbar: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    val studioStrings = strings.mediaStudio
    val context = LocalContext.current

    var isLiveMode by remember(initialMode) { mutableStateOf(initialMode == "live") }
    var useFrontCamera by remember { mutableStateOf(true) }
    var editDraft by remember {
        mutableStateOf(
            MediaEditDraft(
                isLiveStream = initialMode == "live"
            )
        )
    }

    var isLiveBroadcasting by remember { mutableStateOf(false) }
    var liveTitle by remember { mutableStateOf("") }
    var liveElapsedSec by remember { mutableIntStateOf(0) }
    var recordingDurationLimitSec by remember { mutableIntStateOf(30) }
    var isSimulatedRecording by remember { mutableStateOf(false) }
    var recordingElapsedSec by remember { mutableIntStateOf(0) }

    val floatingMojis = remember { mutableStateListOf<LiveFloatingMoji>() }
    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }

    var hasCameraPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasAudioPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasCameraPerm = result[Manifest.permission.CAMERA] == true || hasCameraPerm
        hasAudioPerm = result[Manifest.permission.RECORD_AUDIO] == true || hasAudioPerm
        if (!hasCameraPerm) {
            onShowSnackbar(strings.cameraPermissionRequired)
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPerm || !hasAudioPerm) {
            permissionsLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
        }
    }

    val videoCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        val savedUri = pendingCaptureUri
        if (success && savedUri != null) {
            onShowSnackbar(studioStrings.recordedVideoReadySnackbar)
            onRecordedVideoReady(
                savedUri.toString(),
                editDraft.copy(
                    isLiveStream = false,
                    timerDurationSec = recordingDurationLimitSec,
                    useFrontCamera = useFrontCamera
                )
            )
        } else {
            onShowSnackbar(strings.cameraPermissionRequired)
        }
    }

    fun launchNativeVideoRecorder() {
        try {
            val captureDir = File(context.cacheDir, "media_capture").apply { mkdirs() }
            val outFile = File(captureDir, "rec_${System.currentTimeMillis()}.mp4")
            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, outFile)
            pendingCaptureUri = contentUri
            videoCaptureLauncher.launch(contentUri)
        } catch (_: Exception) {
            // Fallback: toggle in-studio timer recording if hardware camera intent isn't present
            isSimulatedRecording = !isSimulatedRecording
        }
    }

    // Live timer loop
    LaunchedEffect(isLiveBroadcasting) {
        if (isLiveBroadcasting) {
            while (true) {
                delay(1000L)
                liveElapsedSec++
            }
        }
    }

    // Studio timer recording loop
    LaunchedEffect(isSimulatedRecording, recordingDurationLimitSec) {
        if (isSimulatedRecording) {
            recordingElapsedSec = 0
            while (isSimulatedRecording && recordingElapsedSec < recordingDurationLimitSec) {
                delay(1000L)
                recordingElapsedSec++
            }
            if (recordingElapsedSec >= recordingDurationLimitSec) {
                isSimulatedRecording = false
                onShowSnackbar(studioStrings.recordedVideoReadySnackbar)
                onRecordedVideoReady(
                    null,
                    editDraft.copy(
                        isLiveStream = false,
                        timerDurationSec = recordingDurationLimitSec,
                        useFrontCamera = useFrontCamera
                    )
                )
            }
        }
    }

    // Clean up old floating mojis
    fun spawnMoji(emoji: String) {
        val moji = LiveFloatingMoji(
            id = UUID.randomUUID().toString(),
            emoji = emoji,
            xFraction = Random.nextFloat()
        )
        floatingMojis.add(moji)
        if (emoji !in editDraft.mojiStickers) {
            editDraft = editDraft.copy(mojiStickers = (editDraft.mojiStickers + emoji).take(6))
        }
        if (floatingMojis.size > 16) {
            floatingMojis.removeAt(0)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0E101C)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Full-screen Camera Viewfinder + Color Filter
                CameraPreviewSurface(
                    useFrontCamera = useFrontCamera,
                    colorFilterMode = ColorFilterMode.fromId(editDraft.colorFilter),
                    hasCameraPermission = hasCameraPerm,
                    modifier = Modifier.fillMaxSize()
                )

                // Floating Live Moji Animation Layer
                FloatingMojiStage(floatingMojis = floatingMojis)

                // Top Studio Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Close (X) Button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .testTag("studio_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = strings.close,
                            tint = Color.White
                        )
                    }

                    // Mode Switch Pills (Live / Recording)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (!isLiveMode) BrandPrimary else Color.Transparent)
                                .clickable {
                                    isLiveMode = false
                                    isLiveBroadcasting = false
                                    editDraft = editDraft.copy(isLiveStream = false)
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("studio_switch_record")
                        ) {
                            Text(
                                text = studioStrings.recordingModeBtn,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isLiveMode) Color(0xFFE53935) else Color.Transparent)
                                .clickable {
                                    isLiveMode = true
                                    isSimulatedRecording = false
                                    editDraft = editDraft.copy(isLiveStream = true)
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("studio_switch_live")
                        ) {
                            Text(
                                text = studioStrings.goLiveBtn,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Flip Camera Button
                    IconButton(
                        onClick = { useFrontCamera = !useFrontCamera },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .testTag("studio_flip_camera_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = studioStrings.flipCameraDesc,
                            tint = Color.White
                        )
                    }
                }

                // Live Broadcast Status Pill
                if (isLiveMode) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 72.dp, start = 16.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(if (isLiveBroadcasting) Color(0xFFE53935) else Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val mins = liveElapsedSec / 60
                        val secs = liveElapsedSec % 60
                        Text(
                            text = if (isLiveBroadcasting) {
                                "${studioStrings.liveBadge} • %02d:%02d • ${studioStrings.liveViewersCount(18 + liveElapsedSec / 3)}".format(mins, secs)
                            } else {
                                studioStrings.goLiveBtn
                            },
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (isSimulatedRecording) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 72.dp, start = 16.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFE53935))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.FiberManualRecord, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "REC ${recordingElapsedSec}s / ${recordingDurationLimitSec}s",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Bottom Controls Dock
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Live Moji Sticker Tap Strip (Works in both Live & Recording)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DEFAULT_MOJI_STICKERS.forEach { emoji ->
                            val isChosen = emoji in editDraft.mojiStickers
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isChosen) BrandPrimary.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.5f)
                                    )
                                    .border(
                                        width = if (isChosen) 2.dp else 1.dp,
                                        color = if (isChosen) BrandPrimary else Color.White.copy(alpha = 0.25f),
                                        shape = CircleShape
                                    )
                                    .clickable { spawnMoji(emoji) }
                                    .testTag("studio_moji_${emoji.hashCode()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = emoji, fontSize = 22.sp)
                            }
                        }
                    }

                    // Color Mode Filters Horizontal Strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ColorFilterMode.entries.forEach { mode ->
                            val selected = editDraft.colorFilter == mode.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (selected) BrandPrimary else Color.Black.copy(alpha = 0.55f)
                                    )
                                    .border(
                                        1.dp,
                                        if (selected) Color.White else Color.White.copy(alpha = 0.25f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { editDraft = editDraft.copy(colorFilter = mode.id) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                    .testTag("studio_filter_${mode.id}")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color(mode.swatchHex))
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = mode.localizedLabel(selectedLanguageCode),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    if (isLiveMode) {
                        OutlinedTextField(
                            value = liveTitle,
                            onValueChange = { liveTitle = it },
                            placeholder = {
                                Text(studioStrings.liveTitlePlaceholder, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFE53935),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("live_title_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = {
                                if (!isLiveBroadcasting) {
                                    isLiveBroadcasting = true
                                    spawnMoji("🔥")
                                    spawnMoji("🌊")
                                } else {
                                    val finalTitle = liveTitle.trim().ifBlank { studioStrings.defaultLivePostText }
                                    onPublishLivePost(
                                        finalTitle,
                                        null,
                                        editDraft.copy(isLiveStream = true)
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isLiveBroadcasting) BrandPrimary else Color(0xFFE53935)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("live_primary_action_button")
                        ) {
                            Icon(
                                imageVector = if (isLiveBroadcasting) Icons.Default.Stop else Icons.Default.LiveTv,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isLiveBroadcasting) studioStrings.endLivePostBtn else studioStrings.startBroadcastBtn,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        // Recording Mode Duration & Speed Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(15, 30, 60).forEach { sec ->
                                val selected = recordingDurationLimitSec == sec
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (selected) BrandPrimary else Color.Black.copy(alpha = 0.55f))
                                        .clickable { recordingDurationLimitSec = sec }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                        .testTag("record_duration_${sec}")
                                ) {
                                    Text("${sec}s", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            listOf(0.5f, 1.0f, 1.5f, 2.0f).forEach { spd ->
                                val selected = editDraft.playbackSpeed == spd
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (selected) BrandAccent else Color.Black.copy(alpha = 0.55f))
                                        .clickable { editDraft = editDraft.copy(playbackSpeed = spd) }
                                        .padding(horizontal = 8.dp, vertical = 8.dp)
                                ) {
                                    Text("${spd}x", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (!isSimulatedRecording) {
                                        isSimulatedRecording = true
                                    } else {
                                        isSimulatedRecording = false
                                        onShowSnackbar(studioStrings.recordedVideoReadySnackbar)
                                        onRecordedVideoReady(
                                            null,
                                            editDraft.copy(
                                                isLiveStream = false,
                                                timerDurationSec = recordingDurationLimitSec,
                                                useFrontCamera = useFrontCamera
                                            )
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(alpha = 0.7f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("studio_timer_record_button")
                            ) {
                                Icon(
                                    imageVector = if (isSimulatedRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                                    contentDescription = null,
                                    tint = if (isSimulatedRecording) Color(0xFFE53935) else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isSimulatedRecording) studioStrings.stopRecordingBtn else studioStrings.startRecordingBtn,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Button(
                                onClick = { launchNativeVideoRecorder() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("studio_camera_record_button")
                            ) {
                                Icon(Icons.Default.Videocam, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = studioStrings.captureVideoCameraBtn,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
