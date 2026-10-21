package com.fandom.auchatstudio

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fandom.auchatstudio.core.MessageType
import com.fandom.auchatstudio.core.ThemePreset
import com.fandom.auchatstudio.data.AppDatabase
import com.fandom.auchatstudio.data.CharacterEntity
import com.fandom.auchatstudio.data.MessageEntity
import com.fandom.auchatstudio.data.StoryEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Runs Room against the real SQLite of each Android version under test. */
@RunWith(AndroidJUnit4::class)
class DatabaseInstrumentedTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = AppDatabase.inMemory(InstrumentationRegistry.getInstrumentation().targetContext)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun foreignKeysCascadeOnDelete() = runBlocking<Unit> {
        val c = db.characterDao().insert(CharacterEntity(name = "A", roleColorHex = 0xFFFF6584))
        val s = db.storyDao().insert(StoryEntity(title = "S", theme = ThemePreset.INSTAGRAM_DM))
        db.messageDao().insert(MessageEntity(storyId = s, characterId = c, text = "x", position = 0))
        db.messageDao().insert(MessageEntity(storyId = s, characterId = null, type = MessageType.NARRATION, text = "n", position = 1))

        db.characterDao().delete(db.characterDao().getById(c)!!)
        assertThat(db.messageDao().getForStory(s).map { it.text }).containsExactly("n")

        db.storyDao().deleteById(s)
        assertThat(db.messageDao().getForStory(s)).isEmpty()
    }

    @Test
    fun enumsAndUnicodeRoundTrip() = runBlocking<Unit> {
        val s = db.storyDao().insert(StoryEntity(title = "Hẹn hò 💜 bí mật", theme = ThemePreset.WEVERSE_DM))
        val story = db.storyDao().observe(s).first()!!
        assertThat(story.title).isEqualTo("Hẹn hò 💜 bí mật")
        assertThat(story.theme).isEqualTo(ThemePreset.WEVERSE_DM)
    }
}
