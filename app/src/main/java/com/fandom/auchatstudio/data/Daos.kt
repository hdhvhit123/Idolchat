package com.fandom.auchatstudio.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {
    @Query("SELECT * FROM characters ORDER BY id ASC")
    fun getAllCharacters(): Flow<List<CharacterEntity>>

    @Query("SELECT * FROM characters ORDER BY id ASC")
    suspend fun getAll(): List<CharacterEntity>

    @Query("SELECT * FROM characters WHERE id = :id")
    suspend fun getById(id: Long): CharacterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(character: CharacterEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(characters: List<CharacterEntity>): List<Long>

    @Update
    suspend fun updateCharacter(character: CharacterEntity)

    @Query("UPDATE characters SET isUser = 0 WHERE id != :keepId")
    suspend fun clearUserFlagExcept(keepId: Long)

    @Delete
    suspend fun delete(character: CharacterEntity)

    @Query("SELECT COUNT(*) FROM characters")
    suspend fun getCount(): Int
}

@Dao
interface StoryDao {
    @Query(
        """
        SELECT s.*,
            (SELECT COUNT(*) FROM messages m WHERE m.storyId = s.id) AS messageCount,
            (SELECT m.text FROM messages m WHERE m.storyId = s.id ORDER BY m.position DESC, m.id DESC LIMIT 1) AS lastText,
            (SELECT m.type FROM messages m WHERE m.storyId = s.id ORDER BY m.position DESC, m.id DESC LIMIT 1) AS lastType
        FROM stories s
        ORDER BY s.updatedAt DESC, s.id DESC
        """,
    )
    fun observeSummaries(): Flow<List<StorySummary>>

    @Query("SELECT * FROM stories WHERE id = :id")
    fun observe(id: Long): Flow<StoryEntity?>

    @Query("SELECT * FROM stories WHERE id = :id")
    suspend fun getById(id: Long): StoryEntity?

    @Query("SELECT COUNT(*) FROM stories")
    suspend fun getCount(): Int

    @Insert
    suspend fun insert(story: StoryEntity): Long

    @Update
    suspend fun update(story: StoryEntity)

    @Query("UPDATE stories SET updatedAt = :time WHERE id = :id")
    suspend fun touch(id: Long, time: Long)

    @Query("DELETE FROM stories WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE storyId = :storyId ORDER BY position ASC, id ASC")
    fun observeForStory(storyId: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE storyId = :storyId ORDER BY position ASC, id ASC")
    suspend fun getForStory(storyId: Long): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getById(id: Long): MessageEntity?

    @Query("SELECT MAX(position) FROM messages WHERE storyId = :storyId")
    suspend fun maxPosition(storyId: Long): Int?

    @Query("SELECT imagePath FROM messages WHERE storyId = :storyId AND imagePath IS NOT NULL")
    suspend fun imagePathsForStory(storyId: Long): List<String>

    @Query("SELECT imagePath FROM messages WHERE characterId = :characterId AND imagePath IS NOT NULL")
    suspend fun imagePathsForCharacter(characterId: Long): List<String>

    @Insert
    suspend fun insert(message: MessageEntity): Long

    @Insert
    suspend fun insertAll(messages: List<MessageEntity>)

    @Update
    suspend fun update(message: MessageEntity)

    @Update
    suspend fun updateAll(messages: List<MessageEntity>)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteById(id: Long)
}
