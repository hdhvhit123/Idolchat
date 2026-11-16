package com.fandom.auchatstudio.data

import android.net.Uri
import androidx.room.withTransaction
import com.fandom.auchatstudio.core.ListOps
import com.fandom.auchatstudio.core.MessageType
import com.fandom.auchatstudio.core.NameRules
import com.fandom.auchatstudio.core.RolePalette
import com.fandom.auchatstudio.core.StoryTemplate
import com.fandom.auchatstudio.core.TemplateRole
import com.fandom.auchatstudio.core.ThemePreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    private val _ready = MutableStateFlow(false)

    /** False until [initialize] has finished, so the UI can show a loader instead of a false empty state. */
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    /**
     * One-time start-up work. [seedSample] should be true only on the very first launch, so a
     * user who deliberately deleted everything doesn't get the sample back.
     */
    suspend fun initialize(seedSample: Boolean) {
        try {
            if (seedSample) seedIfEmpty()
            cleanupOrphanImages()
        } finally {
            _ready.value = true
        }
    }

    /**
     * Deletes stored images that no character or message refers to any more (left behind by a
     * cancelled undo or an interrupted save). Recent files are skipped in case a save is in flight.
     * Returns the number of files removed.
     */
    suspend fun cleanupOrphanImages(minAgeMillis: Long = ORPHAN_MIN_AGE_MS): Int {
        val candidates = images.listOlderThan(clock() - minAgeMillis)
        if (candidates.isEmpty()) return 0
        val referenced = (characterDao.allAvatarPaths() + messageDao.allImagePaths()).toSet()
        val orphans = candidates.filterNot { it in referenced }
        orphans.forEach { images.delete(it) }
        return orphans.size
    }

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

    /** Copies a story and all its messages (with their own image files). Returns the new id. */
    suspend fun duplicateStory(id: Long): Long? {
        val story = storyDao.getById(id) ?: return null
        val messages = messageDao.getForStory(id)
        // Copy files first: it is slow IO and must not run inside the database transaction.
        val copies = messages.map { m -> m.imagePath?.let { images.copy(it, "msg_copy") } }
        val now = clock()
        return db.withTransaction {
            val title = (NameRules.normalizeName("${story.title} (bản sao)", NameRules.MAX_TITLE_LENGTH) ?: story.title)
            val newId = storyDao.insert(story.copy(id = 0, title = title, createdAt = now, updatedAt = now))
            messageDao.insertAll(
                messages.mapIndexedNotNull { index, m ->
                    // An image whose file could not be copied is dropped rather than left dangling.
                    if (m.type == MessageType.IMAGE && copies[index] == null) {
                        null
                    } else {
                        m.copy(id = 0, storyId = newId, imagePath = copies[index])
                    }
                },
            )
            newId
        }
    }

    /**
     * Creates a story from a built-in template, mapping its roles onto the user's cast. Missing
     * roles get a neutral placeholder character the user can rename later.
     */
    suspend fun createStoryFromTemplate(template: StoryTemplate): Long = db.withTransaction {
        val cast = characterDao.getAll().toMutableList()
        suspend fun create(name: String, isUser: Boolean): CharacterEntity {
            val color = RolePalette.nextFree(cast.map { it.roleColorHex })
            val entity = CharacterEntity(name = name, roleColorHex = color, isUser = isUser)
            return entity.copy(id = characterDao.insert(entity)).also { cast += it }
        }
        val roles = template.lines.map { it.role }.toSet()
        val user = cast.firstOrNull { it.isUser } ?: create("Y/N (Bạn)", isUser = true)
        val idol = cast.firstOrNull { !it.isUser } ?: create("Idol", isUser = false)
        val friend = if (TemplateRole.FRIEND in roles) {
            cast.firstOrNull { !it.isUser && it.id != idol.id } ?: create("Thành viên", isUser = false)
        } else {
            idol
        }
        val storyId = storyDao.insert(StoryEntity(title = template.title, theme = template.theme, createdAt = clock()))
        messageDao.insertAll(
            template.lines.mapIndexed { index, line ->
                MessageEntity(
                    storyId = storyId,
                    characterId = when (line.role) {
                        TemplateRole.IDOL -> idol.id
                        TemplateRole.FRIEND -> friend.id
                        TemplateRole.USER -> user.id
                        TemplateRole.NARRATOR -> null
                    },
                    type = line.type,
                    text = line.text,
                    voiceSeconds = line.voiceSeconds,
                    timeLabel = line.timeLabel,
                    position = index,
                )
            },
        )
        storyId
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

    /**
     * Deletes a message. Pass [deleteImage] = false while an undo is still on offer, then call
     * [discardImage] (or [restoreMessage]) once the user has decided.
     */
    suspend fun deleteMessage(message: MessageEntity, deleteImage: Boolean = true) {
        db.withTransaction {
            messageDao.deleteById(message.id)
            storyDao.touch(message.storyId, clock())
        }
        if (deleteImage) images.delete(message.imagePath)
    }

    /** Puts back a message removed with [deleteMessage]; it returns to its old place in the script. */
    suspend fun restoreMessage(message: MessageEntity): Boolean = db.withTransaction {
        if (storyDao.getById(message.storyId) == null) return@withTransaction false
        if (messageDao.getById(message.id) != null) return@withTransaction true
        // The speaker may have been deleted in the meantime; a dangling id would break the foreign key.
        val speakerExists = message.characterId?.let { characterDao.getById(it) != null } ?: true
        if (!speakerExists) return@withTransaction false
        messageDao.insert(message)
        storyDao.touch(message.storyId, clock())
        true
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

    private companion object {
        const val ORPHAN_MIN_AGE_MS = 60 * 60 * 1000L
    }
}
