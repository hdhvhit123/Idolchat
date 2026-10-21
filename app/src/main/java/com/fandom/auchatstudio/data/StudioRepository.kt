package com.fandom.auchatstudio.data

import android.net.Uri
import androidx.room.withTransaction
import com.fandom.auchatstudio.core.ListOps
import com.fandom.auchatstudio.core.MessageType
import com.fandom.auchatstudio.core.ThemePreset
import kotlinx.coroutines.flow.Flow

class StudioRepository(
    private val db: AppDatabase,
    private val images: ImageStore,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val characterDao = db.characterDao()
    private val storyDao = db.storyDao()
    private val messageDao = db.messageDao()

    val characters: Flow<List<CharacterEntity>> = characterDao.getAllCharacters()
    val stories: Flow<List<StorySummary>> = storyDao.observeSummaries()

    fun story(id: Long): Flow<StoryEntity?> = storyDao.observe(id)
    fun messages(storyId: Long): Flow<List<MessageEntity>> = messageDao.observeForStory(storyId)

    // region Seed

    /** Creates the neutral sample cast and a demo script the first time the app opens. */
    suspend fun seedIfEmpty() {
        db.withTransaction {
            if (characterDao.getCount() > 0 || storyDao.getCount() > 0) return@withTransaction
            val (minho, hyunjin, me) = characterDao.insertAll(SampleData.characters)
            val storyId = storyDao.insert(SampleData.story(clock()))
            messageDao.insertAll(SampleData.messages(storyId, minho, hyunjin, me))
        }
    }

    // endregion

    // region Characters

    suspend fun saveCharacter(
        existing: CharacterEntity?,
        name: String,
        roleColorHex: Long,
        isUser: Boolean,
        avatarPath: String?,
    ): Long {
        val id = db.withTransaction {
            val id = if (existing == null) {
                characterDao.insert(
                    CharacterEntity(name = name, roleColorHex = roleColorHex, isUser = isUser, avatarPath = avatarPath),
                )
            } else {
                characterDao.updateCharacter(
                    existing.copy(name = name, roleColorHex = roleColorHex, isUser = isUser, avatarPath = avatarPath),
                )
                existing.id
            }
            // Only one character can be "me", otherwise both sides of the chat get confusing.
            if (isUser) characterDao.clearUserFlagExcept(id)
            id
        }
        val oldAvatar = existing?.avatarPath
        if (oldAvatar != null && oldAvatar != avatarPath) images.delete(oldAvatar)
        return id
    }

    suspend fun deleteCharacter(character: CharacterEntity) {
        val files = messageDao.imagePathsForCharacter(character.id) + listOfNotNull(character.avatarPath)
        characterDao.delete(character)
        files.forEach { images.delete(it) }
    }

    // endregion

    // region Stories

    suspend fun createStory(title: String, theme: ThemePreset): Long =
        storyDao.insert(StoryEntity(title = title, theme = theme, createdAt = clock()))

    suspend fun renameStory(id: Long, title: String) {
        val story = storyDao.getById(id) ?: return
        storyDao.update(story.copy(title = title, updatedAt = clock()))
    }

    suspend fun setChatTitle(id: Long, chatTitle: String?) {
        val story = storyDao.getById(id) ?: return
        storyDao.update(story.copy(chatTitle = chatTitle, updatedAt = clock()))
    }

    suspend fun setTheme(id: Long, theme: ThemePreset) {
        val story = storyDao.getById(id) ?: return
        storyDao.update(story.copy(theme = theme, updatedAt = clock()))
    }

    suspend fun deleteStory(id: Long) {
        val files = messageDao.imagePathsForStory(id)
        storyDao.deleteById(id)
        files.forEach { images.delete(it) }
    }

    // endregion

    // region Messages

    suspend fun addMessage(
        storyId: Long,
        characterId: Long?,
        type: MessageType,
        text: String = "",
        voiceSeconds: Int = 0,
        imagePath: String? = null,
        timeLabel: String? = null,
    ): Long = db.withTransaction {
        val position = (messageDao.maxPosition(storyId) ?: -1) + 1
        val id = messageDao.insert(
            MessageEntity(
                storyId = storyId,
                characterId = if (type == MessageType.NARRATION) null else characterId,
                type = type,
                text = text,
                voiceSeconds = voiceSeconds,
                imagePath = imagePath,
                timeLabel = timeLabel,
                position = position,
            ),
        )
        storyDao.touch(storyId, clock())
        id
    }

    suspend fun updateMessage(message: MessageEntity) {
        db.withTransaction {
            messageDao.update(message)
            storyDao.touch(message.storyId, clock())
        }
    }

    suspend fun deleteMessage(message: MessageEntity) {
        db.withTransaction {
            messageDao.deleteById(message.id)
            storyDao.touch(message.storyId, clock())
        }
        images.delete(message.imagePath)
    }

    /** Moves a message within its story and rewrites positions so they stay 0..n-1. */
    suspend fun moveMessage(storyId: Long, fromIndex: Int, toIndex: Int) {
        db.withTransaction {
            val current = messageDao.getForStory(storyId)
            val moved = ListOps.move(current, fromIndex, toIndex)
            if (moved === current) return@withTransaction
            val changed = moved.mapIndexedNotNull { index, msg -> if (msg.position != index) msg.copy(position = index) else null }
            messageDao.updateAll(changed)
            storyDao.touch(storyId, clock())
        }
    }

    suspend fun importImage(uri: Uri, prefix: String, maxSize: Int = ImageStore.DEFAULT_MAX_SIZE): String? =
        images.importImage(uri, prefix, maxSize)

    suspend fun discardImage(path: String?) = images.delete(path)

    // endregion
}
