package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

data class ProcessedMedia(
    val mediaUri: String,
    val thumbnailUri: String?,
    val isVideo: Boolean
)

object MediaProcessor {
    private const val MAX_IMAGE_DIMENSION = 1600
    private const val COPY_BUFFER_SIZE = 32 * 1024

    fun isVideoUri(context: Context, uriString: String?, requestedType: String = ""): Boolean {
        if (uriString.isNullOrBlank()) return requestedType == "reels"
        val uri = Uri.parse(uriString)
        val mime = try {
            context.contentResolver.getType(uri)
        } catch (_: Exception) {
            null
        }
        if (mime != null) {
            if (mime.startsWith("video/")) return true
            if (mime.startsWith("image/")) return false
        }
        val lower = uriString.lowercase()
        return lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".webm") ||
            lower.endsWith(".mov") || lower.endsWith(".3gp") || requestedType == "reels"
    }

    suspend fun extractVideoThumbnailPreview(context: Context, uriString: String): String? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "file" && uri.path != null) {
                retriever.setDataSource(uri.path)
            } else {
                retriever.setDataSource(context, uri)
            }
            val frame = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime
            if (frame != null) {
                val thumbFile = File(context.cacheDir, "vid_preview_${System.currentTimeMillis()}.jpg")
                FileOutputStream(thumbFile).use { out ->
                    frame.compress(Bitmap.CompressFormat.JPEG, 82, out)
                }
                frame.recycle()
                Uri.fromFile(thumbFile).toString()
            } else null
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    suspend fun processAndSaveMedia(
        context: Context,
        sourceUriString: String,
        requestedType: String,
        onProgress: (Float) -> Unit
    ): ProcessedMedia = withContext(Dispatchers.IO) {
        val mediaDir = File(context.filesDir, "media").apply { mkdirs() }
        val isVideo = isVideoUri(context, sourceUriString, requestedType)
        val uri = Uri.parse(sourceUriString)

        if (isVideo) {
            onProgress(0.15f)
            val destFile = File(mediaDir, "vid_${System.currentTimeMillis()}.mp4")
            copyUriToFile(context, uri, destFile) { copyFrac ->
                onProgress(0.15f + copyFrac * 0.65f)
            }
            onProgress(0.85f)
            val thumbUri = extractAndSaveThumbnail(destFile, mediaDir)
            onProgress(1.0f)
            ProcessedMedia(
                mediaUri = Uri.fromFile(destFile).toString(),
                thumbnailUri = thumbUri,
                isVideo = true
            )
        } else {
            onProgress(0.2f)
            val destFile = File(mediaDir, "img_${System.currentTimeMillis()}.jpg")
            val compressed = decodeSampledAndSaveImage(context, uri, destFile)
            onProgress(0.75f)
            if (!compressed) {
                copyUriToFile(context, uri, destFile) { copyFrac ->
                    onProgress(0.5f + copyFrac * 0.45f)
                }
            }
            onProgress(1.0f)
            val finalUri = Uri.fromFile(destFile).toString()
            ProcessedMedia(
                mediaUri = finalUri,
                thumbnailUri = finalUri,
                isVideo = false
            )
        }
    }

    suspend fun persistImageToFilesDir(
        context: Context,
        sourceUriString: String,
        prefix: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val mediaDir = File(context.filesDir, "profile_media").apply { mkdirs() }
            val destFile = File(mediaDir, "${prefix}_${System.currentTimeMillis()}.jpg")
            val uri = Uri.parse(sourceUriString)
            val ok = decodeSampledAndSaveImage(context, uri, destFile)
            if (!ok) {
                copyUriToFile(context, uri, destFile) {}
            }
            if (destFile.exists() && destFile.length() > 0L) {
                Uri.fromFile(destFile).toString()
            } else {
                sourceUriString
            }
        } catch (_: Exception) {
            sourceUriString
        }
    }

    private fun extractAndSaveThumbnail(videoFile: File, mediaDir: File): String? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(videoFile.absolutePath)
            val frame = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime
            if (frame != null) {
                val thumbFile = File(mediaDir, "thumb_${System.currentTimeMillis()}.jpg")
                FileOutputStream(thumbFile).use { out ->
                    frame.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                frame.recycle()
                Uri.fromFile(thumbFile).toString()
            } else null
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun openUriStream(context: Context, uri: Uri): InputStream? {
        return if (uri.scheme == "file" && uri.path != null) {
            FileInputStream(File(uri.path!!))
        } else {
            context.contentResolver.openInputStream(uri)
        }
    }

    private fun copyUriToFile(
        context: Context,
        uri: Uri,
        destFile: File,
        onCopyProgress: (Float) -> Unit
    ) {
        val totalBytes = try {
            if (uri.scheme == "file" && uri.path != null) {
                File(uri.path!!).length()
            } else {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
            }
        } catch (_: Exception) {
            -1L
        }

        openUriStream(context, uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                val buffer = ByteArray(COPY_BUFFER_SIZE)
                var copied = 0L
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    copied += read
                    if (totalBytes > 0L) {
                        onCopyProgress((copied.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f))
                    }
                }
                output.flush()
            }
        } ?: throw IllegalStateException("Cannot read selected media file")
    }

    private fun decodeSampledAndSaveImage(context: Context, uri: Uri, destFile: File): Boolean {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            openUriStream(context, uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return false

            var inSampleSize = 1
            var halfW = bounds.outWidth / 2
            var halfH = bounds.outHeight / 2
            while (halfW / inSampleSize >= MAX_IMAGE_DIMENSION || halfH / inSampleSize >= MAX_IMAGE_DIMENSION) {
                inSampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
            val bitmap = openUriStream(context, uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return false

            FileOutputStream(destFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
                out.flush()
            }
            bitmap.recycle()
            true
        } catch (_: Exception) {
            false
        }
    }
}
