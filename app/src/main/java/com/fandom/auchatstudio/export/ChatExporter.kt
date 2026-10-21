package com.fandom.auchatstudio.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.fandom.auchatstudio.data.CharacterEntity
import com.fandom.auchatstudio.ui.chat.ChatItem
import com.fandom.auchatstudio.ui.chat.ChatStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

interface ExportService {
    /** Renders the conversation as one or more long screenshots and returns their gallery Uris. */
    suspend fun export(
        title: String,
        headerTitle: String,
        partners: List<CharacterEntity>,
        items: List<ChatItem>,
        showNames: Boolean,
        style: ChatStyle,
    ): List<Uri>
}

/**
 * "Long Screenshot" export (roadmap item): the whole chat is drawn on an off-screen Canvas,
 * split into pages that fit in memory, watermarked and saved to Pictures/AU Chat Studio.
 * MediaStore inserts need no storage permission on Android 10+.
 */
class ChatExporter(private val context: Context) : ExportService {

    override suspend fun export(
        title: String,
        headerTitle: String,
        partners: List<CharacterEntity>,
        items: List<ChatItem>,
        showNames: Boolean,
        style: ChatStyle,
    ): List<Uri> {
        val pages = withContext(Dispatchers.Default) {
            ChatImageRenderer(context, style, showNames).render(headerTitle, partners, items)
        }
        return withContext(Dispatchers.IO) {
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val base = safeFileName(title)
            pages.mapIndexedNotNull { index, bitmap ->
                val suffix = if (pages.size > 1) "_${index + 1}" else ""
                try {
                    save(bitmap, "${base}_$stamp$suffix.png")
                } finally {
                    bitmap.recycle()
                }
            }
        }
    }

    private fun save(bitmap: Bitmap, displayName: String): Uri? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/AU Chat Studio")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = resolver.insert(collection, values) ?: return null
        return try {
            val written = resolver.openOutputStream(uri)?.use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) } ?: false
            if (!written) {
                resolver.delete(uri, null, null)
                return null
            }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            null
        }
    }

    companion object {
        fun safeFileName(title: String): String {
            val cleaned = title.replace(Regex("[^\\p{L}\\p{M}\\p{N}]+"), "_").trim('_')
            return cleaned.take(40).ifEmpty { "AU_Chat" }
        }
    }
}
