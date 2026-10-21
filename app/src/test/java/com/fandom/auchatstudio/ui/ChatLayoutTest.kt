package com.fandom.auchatstudio.ui

import com.fandom.auchatstudio.core.MessageType
import com.fandom.auchatstudio.core.ThemePreset
import com.fandom.auchatstudio.data.CharacterEntity
import com.fandom.auchatstudio.data.MessageEntity
import com.fandom.auchatstudio.ui.chat.ChatLayout
import com.fandom.auchatstudio.ui.chat.ChatStyle
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ChatLayoutTest {
    private val idol = CharacterEntity(id = 1, name = "Minho", roleColorHex = 1)
    private val friend = CharacterEntity(id = 2, name = "Hyunjin", roleColorHex = 2)
    private val me = CharacterEntity(id = 3, name = "Y/N", roleColorHex = 3, isUser = true)
    private val cast = listOf(idol, friend, me)
    private var pos = 0
    private fun m(speaker: Long?, type: MessageType = MessageType.TEXT) =
        MessageEntity(id = pos.toLong() + 1, storyId = 1, characterId = speaker, type = type, text = "t$pos", position = pos++)

    @Test
    fun runsAreGroupedPerSpeakerAndNarrationBreaksThem() {
        val items = ChatLayout.build(
            listOf(m(1), m(1), m(3), m(null, MessageType.NARRATION), m(3), m(1)),
            cast,
        )
        assertThat(items.map { it.firstInRun }).containsExactly(true, false, true, true, true, true).inOrder()
        assertThat(items.map { it.lastInRun }).containsExactly(false, true, true, true, true, true).inOrder()
        assertThat(items.map { it.outgoing }).containsExactly(false, false, true, false, true, false).inOrder()
    }

    @Test
    fun unknownSpeakerRendersAsIncoming() {
        val items = ChatLayout.build(listOf(m(99)), cast)
        assertThat(items.single().speaker).isNull()
        assertThat(items.single().outgoing).isFalse()
    }

    @Test
    fun groupDetectionIgnoresTheUser() {
        assertThat(ChatLayout.isGroup(listOf(m(1), m(3)), cast)).isFalse()
        assertThat(ChatLayout.isGroup(listOf(m(1), m(2), m(3)), cast)).isTrue()
    }

    @Test
    fun headerTitlePrefersExplicitTitleThenPartners() {
        assertThat(ChatLayout.headerTitle("Bubble", emptyList(), cast)).isEqualTo("Bubble")
        assertThat(ChatLayout.headerTitle(null, listOf(m(1), m(3), m(2)), cast)).isEqualTo("Minho, Hyunjin")
        assertThat(ChatLayout.headerTitle("  ", emptyList(), cast)).isEqualTo("Cuộc trò chuyện")
    }

    @Test
    fun everyPresetHasAStyleMatchingTheDesignPalette() {
        ThemePreset.entries.forEach { assertThat(ChatStyle.of(it).preset).isEqualTo(it) }
        assertThat(ChatStyle.Lysn.accent).isEqualTo(0xFF7CB8EB)
        assertThat(ChatStyle.Lysn.background).isEqualTo(0xFFF3F6FB)
        assertThat(ChatStyle.Lysn.cornerDp).isEqualTo(18f)
        assertThat(ChatStyle.Weverse.background).isEqualTo(0xFF121214)
        assertThat(ChatStyle.Weverse.accent).isEqualTo(0xFF00E676)
        assertThat(ChatStyle.Instagram.background).isEqualTo(0xFF000000)
        assertThat(ChatStyle.Instagram.outgoingGradient.size).isAtLeast(2)
    }
}
