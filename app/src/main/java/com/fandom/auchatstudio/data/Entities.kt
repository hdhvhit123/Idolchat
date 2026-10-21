package com.fandom.auchatstudio.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fandom.auchatstudio.core.MessageType
import com.fandom.auchatstudio.core.ThemePreset

@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Compose colour stored as an ARGB Long, e.g. 0xFFFF6584. */
    val roleColorHex: Long,
    /** The reader's own character: their messages sit on the right. */
    val isUser: Boolean = false,
    /** Absolute path inside context.filesDir (never a content:// Uri, see FileStore). */
    val avatarPath: String? = null,
)

@Entity(tableName = "stories")
data class StoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val theme: ThemePreset = ThemePreset.Default,
    /** Name shown in the fake chat header. Null means "use the participants' names". */
    val chatTitle: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
)

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = StoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["storyId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CharacterEntity::class,
            parentColumns = ["id"],
            childColumns = ["characterId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("storyId"), Index("characterId")],
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val storyId: Long,
    /** Null only for narration lines. */
    val characterId: Long?,
    val type: MessageType = MessageType.TEXT,
    val text: String = "",
    val voiceSeconds: Int = 0,
    val imagePath: String? = null,
    /** Optional "23:41"-style timestamp shown under the bubble. */
    val timeLabel: String? = null,
    val position: Int,
)

/** Row for the home screen list. */
data class StorySummary(
    @Embedded val story: StoryEntity,
    @ColumnInfo(name = "messageCount") val messageCount: Int,
    @ColumnInfo(name = "lastText") val lastText: String?,
    @ColumnInfo(name = "lastType") val lastType: MessageType?,
)
