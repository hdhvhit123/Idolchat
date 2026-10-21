package com.fandom.auchatstudio

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fandom.auchatstudio.core.MessageType
import com.fandom.auchatstudio.data.CharacterEntity
import com.fandom.auchatstudio.data.MessageEntity
import com.fandom.auchatstudio.export.ChatExporter
import com.fandom.auchatstudio.export.ChatImageRenderer
import com.fandom.auchatstudio.ui.chat.ChatLayout
import com.fandom.auchatstudio.ui.chat.ChatStyle
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

/** Saves a real long screenshot through MediaStore (no permission needed on API 29+). */
@RunWith(AndroidJUnit4::class)
class ExporterInstrumentedTest {
    @Test
    fun exportWritesReadablePngToGallery() = runBlocking<Unit> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val idol = CharacterEntity(id = 1, name = "Minho", roleColorHex = 0xFFFF6584)
        val me = CharacterEntity(id = 2, name = "Y/N", roleColorHex = 0xFF43D8C9, isUser = true)
        val messages = listOf(
            MessageEntity(id = 1, storyId = 1, characterId = 1, text = "Còn thức không?", position = 0),
            MessageEntity(id = 2, storyId = 1, characterId = 1, type = MessageType.VOICE, voiceSeconds = 12, position = 1),
            MessageEntity(id = 3, storyId = 1, characterId = 2, text = "Dạ còn 👀", timeLabel = "02:14", position = 2),
        )
        val uris = ChatExporter(context).export(
            title = "Instrumented",
            headerTitle = "Minho",
            partners = listOf(idol),
            items = ChatLayout.build(messages, listOf(idol, me)),
            showNames = false,
            style = ChatStyle.Instagram,
        )
        try {
            assertThat(uris).hasSize(1)
            val bitmap = context.contentResolver.openInputStream(uris[0]).use { BitmapFactory.decodeStream(it) }
            assertThat(bitmap).isNotNull()
            assertThat(bitmap.width).isEqualTo(ChatImageRenderer.WIDTH_PX.toInt())
        } finally {
            uris.forEach { context.contentResolver.delete(it, null, null) }
        }
    }
}
