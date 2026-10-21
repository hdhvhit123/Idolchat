package com.fandom.auchatstudio.data

import com.fandom.auchatstudio.core.MessageType
import com.fandom.auchatstudio.core.ThemePreset

/**
 * Neutral placeholder cast. Per the design document's legal rules the APK ships no real idol
 * photos or trademarked names beyond generic first names; users load their own avatars.
 */
internal object SampleData {
    val characters = listOf(
        CharacterEntity(name = "Minho (Bias)", roleColorHex = 0xFFFF6584, isUser = false),
        CharacterEntity(name = "Hyunjin", roleColorHex = 0xFF6C63FF, isUser = false),
        CharacterEntity(name = "Y/N (Bạn)", roleColorHex = 0xFF43D8C9, isUser = true),
    )

    fun story(now: Long) = StoryEntity(
        title = "Tin nhắn lúc nửa đêm",
        theme = ThemePreset.LYSN_BUBBLE,
        chatTitle = "Minho",
        createdAt = now,
    )

    fun messages(storyId: Long, minho: Long, hyunjin: Long, me: Long): List<MessageEntity> {
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
        return listOf(
            msg(null, "02:13 · Minho đã gửi cho bạn một tin nhắn", MessageType.NARRATION),
            msg(minho, "Còn thức không đó?", time = "02:13"),
            msg(me, "Sao anh biết em chưa ngủ 👀", time = "02:14"),
            msg(minho, "Đoán thôi. Mà đoán đúng ha"),
            msg(minho, type = MessageType.VOICE, voiceSeconds = 12, time = "02:15"),
            msg(me, "Anh hát đó hả?? Bài mới sao 😳"),
            msg(minho, "Bí mật. Đừng nói ai nhé", time = "02:16"),
            msg(hyunjin, "Hai người nhắn gì lén lút vậy 😏"),
            msg(null, "Hyunjin đã xem", MessageType.NARRATION),
        )
    }
}
