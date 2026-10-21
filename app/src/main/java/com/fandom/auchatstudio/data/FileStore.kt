package com.fandom.auchatstudio.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

interface ImageStore {
    /**
     * Copies a picture picked with the Photo Picker into app-private storage and returns its
     * absolute path, or null if it could not be read.
     */
    suspend fun importImage(source: Uri, prefix: String, maxSize: Int = DEFAULT_MAX_SIZE): String?

    /** Deletes a file previously returned by [importImage]. Paths outside our folder are ignored. */
    suspend fun delete(path: String?)

    companion object {
        const val DEFAULT_MAX_SIZE = 1600
        const val AVATAR_MAX_SIZE = 512
    }
}

/**
 * Photo Picker Uris lose their read grant after a reboot, so (as the design document requires)
 * every picked image is copied into context.filesDir. Images are downscaled on the way in so
 * the chat list stays smooth and storage use stays small.
 */
class FileStore(private val context: Context) : ImageStore {

    private val imageDir: File get() = File(context.filesDir, "images").apply { mkdirs() }

    override suspend fun importImage(source: Uri, prefix: String, maxSize: Int): String? =
        withContext(Dispatchers.IO) {
            val target = File(imageDir, "${prefix}_${System.currentTimeMillis()}.webp")
            try {
                val bitmap = ImageDecoder.decodeBitmap(
                    ImageDecoder.createSource(context.contentResolver, source),
                ) { decoder, info, _ ->
                    val w = info.size.width
                    val h = info.size.height
                    val longest = max(w, h)
                    if (longest > maxSize) {
                        val scale = maxSize.toFloat() / longest
                        decoder.setTargetSize(
                            (w * scale).roundToInt().coerceAtLeast(1),
                            (h * scale).roundToInt().coerceAtLeast(1),
                        )
                    }
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
                FileOutputStream(target).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 88, out)
                }
                bitmap.recycle()
                target.absolutePath
            } catch (e: Exception) {
                Log.w(TAG, "Decode failed, falling back to a raw copy", e)
                rawCopy(source, target)
            }
        }

    private fun rawCopy(source: Uri, target: File): String? = try {
        val input = context.contentResolver.openInputStream(source)
        if (input == null) {
            null
        } else {
            input.use { stream -> FileOutputStream(target).use { stream.copyTo(it) } }
            target.absolutePath
        }
    } catch (e: Exception) {
        Log.e(TAG, "Could not copy picked image", e)
        target.delete()
        null
    }

    override suspend fun delete(path: String?) {
        if (path == null) return
        withContext(Dispatchers.IO) {
            val file = File(path)
            val root = context.filesDir.canonicalPath
            if (file.canonicalPath.startsWith(root)) file.delete()
        }
    }

    private companion object {
        const val TAG = "FileStore"
    }
}
