package com.fandom.auchatstudio.data

import com.fandom.auchatstudio.core.MessageType
import com.fandom.auchatstudio.core.ThemePreset

/**
 * Neutral placeholder cast. Per the design document's legal rules the APK ships no real idol
 * photos or trademarked names beyond generic first names; users load their own avatars.
 */
internal object SampleData {
    fun characters(strings: RepositoryStrings) = listOf(
        CharacterEntity(name = "Minho (Bias)", roleColorHex = 0xFFFF6584, isUser = false),
        CharacterEntity(name = "Hyunjin", roleColorHex = 0xFF6C63FF, isUser = false),
        CharacterEntity(name = strings.defaultUserName, roleColorHex = 0xFF43D8C9, isUser = true),
    )

    fun story(strings: RepositoryStrings, now: Long) = StoryEntity(
        title = strings.sampleTitle,
        theme = ThemePreset.LYSN_BUBBLE,
        chatTitle = "Minho",
        createdAt = now,
    )

    fun messages(strings: RepositoryStrings, storyId: Long, minho: Long, hyunjin: Long, me: Long): List<MessageEntity> {
        var position = 0
        fun msg(
            characterId: Long?,
            text: String = "",
            type: MessageType = MessageType.TEXT,
            voiceSeconds: Int = 0,
            time: String? = null,
        ) = MessageEntity(
            storyId = storyId,
            characterId = characterId,
            type = type,
            text = text,
            voiceSeconds = voiceSeconds,
            timeLabel = time,
            position = position++,
        )
        // Eight written lines; a translation with any other count falls back to English.
        val lines = strings.sampleLines.takeIf { it.size == PlainRepositoryStrings.sampleLines.size }
            ?: PlainRepositoryStrings.sampleLines
        return listOf(
            msg(null, lines[0], MessageType.NARRATION),
            msg(minho, lines[1], time = "02:13"),
            msg(me, lines[2], time = "02:14"),
            msg(minho, lines[3]),
            msg(minho, type = MessageType.VOICE, voiceSeconds = 12, time = "02:15"),
            msg(me, lines[4]),
            msg(minho, lines[5], time = "02:16"),
            msg(hyunjin, lines[6]),
            msg(null, lines[7], MessageType.NARRATION),
        )
    }
}
