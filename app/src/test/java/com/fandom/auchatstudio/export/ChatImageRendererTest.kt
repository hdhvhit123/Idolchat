package com.fandom.auchatstudio.export

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fandom.auchatstudio.core.MessageType
import com.fandom.auchatstudio.data.CharacterEntity
import com.fandom.auchatstudio.data.MessageEntity
import com.fandom.auchatstudio.ui.chat.ChatLayout
import com.fandom.auchatstudio.ui.chat.ChatStyle
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ChatImageRendererTest {
    private val idol = CharacterEntity(id = 1, name = "Minho", roleColorHex = 0xFFFF6584)
    private val me = CharacterEntity(id = 2, name = "Y/N", roleColorHex = 0xFF43D8C9, isUser = true)

    private fun messages(n: Int) = (0 until n).map { i ->
        val type = when (i % 4) {
            1 -> MessageType.VOICE
            3 -> MessageType.NARRATION
            else -> MessageType.TEXT
        }
        MessageEntity(
            id = i + 1L,
            storyId = 1,
            characterId = if (type == MessageType.NARRATION) null else if (i % 2 == 0) 1 else 2,
            type = type,
            text = "Tin nhắn số $i với một chút nội dung dài hơn để xuống dòng thử xem sao",
            voiceSeconds = 7,
            timeLabel = if (i % 5 == 0) "23:4${i % 10}" else null,
            position = i,
        )
    }

    private fun render(n: Int, style: ChatStyle) = ChatImageRenderer(
        ApplicationProvider.getApplicationContext(),
        style,
        showNames = false,
    ).render("Minho", listOf(idol), ChatLayout.build(messages(n), listOf(idol, me)))

    @Test
    fun shortChatRendersOneFullWidthPage() {
        val pages = render(4, ChatStyle.Lysn)
        assertThat(pages).hasSize(1)
        assertThat(pages[0].width).isEqualTo(ChatImageRenderer.WIDTH_PX.toInt())
        assertThat(pages[0].height).isGreaterThan(400)
        // Background colour of the preset is used below the header.
        assertThat(pages[0].getPixel(2, pages[0].height / 2)).isEqualTo(ChatStyle.Lysn.background.toInt())
    }

    @Test
    fun longChatIsSplitIntoBoundedPages() {
        val pages = render(220, ChatStyle.Weverse)
        assertThat(pages.size).isGreaterThan(1)
        pages.forEach { assertThat(it.height).isAtMost(ChatImageRenderer.MAX_PAGE_HEIGHT_PX + 200) }
    }

    @Test
    fun everyPresetRenders() {
        listOf(ChatStyle.Lysn, ChatStyle.Weverse, ChatStyle.Instagram).forEach { style ->
            assertThat(render(8, style)).isNotEmpty()
        }
    }

    @Test
    fun fileNamesAreSafe() {
        assertThat(ChatExporter.safeFileName("Hẹn hò / bí mật: 2h sáng!")).isEqualTo("Hẹn_hò_bí_mật_2h_sáng")
        assertThat(ChatExporter.safeFileName("///")).isEqualTo("AU_Chat")
    }
}
