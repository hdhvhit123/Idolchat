package com.fandom.auchatstudio.ui.chat

import com.fandom.auchatstudio.core.MessageType
import com.fandom.auchatstudio.data.CharacterEntity
import com.fandom.auchatstudio.data.MessageEntity

/** A message resolved against its speaker plus the grouping info the bubble layout needs. */
data class ChatItem(
    val message: MessageEntity,
    val speaker: CharacterEntity?,
    /** Right-aligned (the reader's own character). */
    val outgoing: Boolean,
    /** First bubble in a run from the same speaker: shows the sender name in group chats. */
    val firstInRun: Boolean,
    /** Last bubble in a run: shows the avatar and the bubble "tail". */
    val lastInRun: Boolean,
) {
    val id: Long get() = message.id
    val isNarration: Boolean get() = message.type == MessageType.NARRATION
}

object ChatLayout {
    /**
     * Resolves speakers and computes bubble runs. Narration breaks a run. A speaker id that no
     * longer exists is rendered as an unknown incoming speaker rather than crashing.
     */
    fun build(messages: List<MessageEntity>, characters: List<CharacterEntity>): List<ChatItem> {
        val byId = characters.associateBy { it.id }
        fun runKey(m: MessageEntity): Long? = if (m.type == MessageType.NARRATION) null else m.characterId ?: -1L
        return messages.mapIndexed { i, m ->
            val speaker = m.characterId?.let(byId::get)
            val key = runKey(m)
            val prevKey = messages.getOrNull(i - 1)?.let(::runKey)
            val nextKey = messages.getOrNull(i + 1)?.let(::runKey)
            ChatItem(
                message = m,
                speaker = speaker,
                outgoing = speaker?.isUser == true,
                firstInRun = key == null || prevKey != key,
                lastInRun = key == null || nextKey != key,
            )
        }
    }

    /** Group chat = more than one non-user speaker, so names are shown above bubbles. */
    fun isGroup(messages: List<MessageEntity>, characters: List<CharacterEntity>): Boolean {
        val users = characters.filter { it.isUser }.map { it.id }.toSet()
        return messages.mapNotNull { it.characterId }.filter { it !in users }.distinct().size > 1
    }

    /** Header title: explicit chat title, else the other participants' names. */
    fun headerTitle(chatTitle: String?, messages: List<MessageEntity>, characters: List<CharacterEntity>): String {
        if (!chatTitle.isNullOrBlank()) return chatTitle
        val byId = characters.associateBy { it.id }
        val names = messages.mapNotNull { it.characterId }.distinct()
            .mapNotNull { byId[it] }
            .filterNot { it.isUser }
            .map { it.name }
        return when {
            names.isEmpty() -> "Cuộc trò chuyện"
            names.size <= 3 -> names.joinToString(", ")
            else -> names.take(2).joinToString(", ") + " +${names.size - 2}"
        }
    }

    /** Partners shown as avatars in the header (excludes the reader). */
    fun partners(messages: List<MessageEntity>, characters: List<CharacterEntity>): List<CharacterEntity> {
        val byId = characters.associateBy { it.id }
        return messages.mapNotNull { it.characterId }.distinct().mapNotNull { byId[it] }.filterNot { it.isUser }
    }
}
