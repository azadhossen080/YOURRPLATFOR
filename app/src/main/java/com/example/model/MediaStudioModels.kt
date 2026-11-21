package com.example.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

enum class ColorFilterMode(
    val id: String,
    val labelEn: String,
    val labelBn: String,
    val swatchHex: Long,
    val overlayHex: Long,
    val overlayAlpha: Float
) {
    NONE("none", "Original", "স্বাভাবিক", 0xFF5352ED, 0x00000000, 0f),
    WARM("warm", "Warm", "ওয়ার্ম", 0xFFFF7043, 0xFFFF7043, 0.22f),
    COOL("cool", "Cool", "কুল", 0xFF29B6F6, 0xFF00B0FF, 0.20f),
    BW("bw", "B&W", "সাদা-কালো", 0xFF78909C, 0xFF1F2933, 0.36f),
    VIVID("vivid", "Vivid", "ভিভিড", 0xFFE91E63, 0xFFD500F9, 0.14f),
    CINEMA("cinema", "Cinema", "সিনেমা", 0xFF009688, 0xFF004D40, 0.26f),
    RETRO("retro", "Retro", "রেট্রো", 0xFFFFB300, 0xFF8D6E63, 0.24f);

    companion object {
        fun fromId(id: String?): ColorFilterMode {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: NONE
        }
    }

    fun colorFilter(): ColorFilter? {
        return when (this) {
            NONE -> null
            WARM -> ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        1.18f, 0f, 0f, 0f, 18f,
                        0f, 1.04f, 0f, 0f, 6f,
                        0f, 0f, 0.86f, 0f, -8f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
            COOL -> ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        0.88f, 0f, 0f, 0f, -6f,
                        0f, 1.05f, 0f, 0f, 8f,
                        0f, 0f, 1.22f, 0f, 20f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
            BW -> ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
            VIVID -> ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(1.48f) })
            CINEMA -> ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        1.10f, -0.04f, 0f, 0f, -6f,
                        0f, 1.04f, 0.04f, 0f, 4f,
                        -0.05f, 0.08f, 1.15f, 0f, 10f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
            RETRO -> ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        0.95f, 0.25f, 0.08f, 0f, 10f,
                        0.10f, 0.88f, 0.08f, 0f, 6f,
                        0.05f, 0.12f, 0.68f, 0f, -4f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }
    }

    fun overlayColor(): Color = Color(overlayHex).copy(alpha = overlayAlpha)
}

enum class TransitionEffect(
    val id: String,
    val labelEn: String,
    val labelBn: String
) {
    NONE("none", "None", "সাধারণ"),
    FADE("fade", "Fade", "ফেড"),
    ZOOM_IN("zoom_in", "Zoom", "জুম"),
    SLIDE_UP("slide_up", "Slide", "স্লাইড"),
    FLASH("flash", "Flash", "ফ্ল্যাশ"),
    SPIN("spin", "Spin", "স্পিন");

    companion object {
        fun fromId(id: String?): TransitionEffect {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: NONE
        }
    }
}

val DEFAULT_MOJI_STICKERS: List<String> = listOf(
    "🔥", "❤️", "😂", "🌊", "✨", "😎", "🎉", "💯", "👑", "🚀", "💥", "🥳"
)

data class LiveFloatingMoji(
    val id: String,
    val emoji: String,
    val xFraction: Float
)

data class MediaEditDraft(
    val colorFilter: String = ColorFilterMode.NONE.id,
    val transitionEffect: String = TransitionEffect.NONE.id,
    val mojiStickers: List<String> = emptyList(),
    val playbackSpeed: Float = 1.0f,
    val isMuted: Boolean = false,
    val trimStartFraction: Float = 0f,
    val trimEndFraction: Float = 1f,
    val isLiveStream: Boolean = false
)
