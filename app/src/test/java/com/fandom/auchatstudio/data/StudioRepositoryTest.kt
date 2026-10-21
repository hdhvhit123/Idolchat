package com.fandom.auchatstudio.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fandom.auchatstudio.FakeImageStore
import com.fandom.auchatstudio.core.MessageType
import com.fandom.auchatstudio.core.ThemePreset
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StudioRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var images: FakeImageStore
    private lateinit var repo: StudioRepository
    private var now = 1_000L

    @Before
    fun setUp() {
        db = AppDatabase.inMemory(ApplicationProvider.getApplicationContext())
        images = FakeImageStore()
        repo = StudioRepository(db, images) { now++ }
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun seedCreatesSampleCastAndStoryOnlyOnce() = runBlocking {
        repo.seedIfEmpty()
        repo.seedIfEmpty()
        val characters = repo.characters.first()
        assertThat(characters.map { it.name }).containsExactly("Minho (Bias)", "Hyunjin", "Y/N (Bạn)").inOrder()
        assertThat(characters.single { it.isUser }.name).isEqualTo("Y/N (Bạn)")
        val stories = repo.stories.first()
        assertThat(stories).hasSize(1)
        assertThat(stories[0].messageCount).isGreaterThan(5)
        assertThat(stories[0].lastType).isEqualTo(MessageType.NARRATION)
    }

    @Test
    fun seedSkipsWhenUserAlreadyHasData() = runBlocking {
        repo.createStory("Của tôi", ThemePreset.WEVERSE_DM)
        repo.seedIfEmpty()
        assertThat(repo.characters.first()).isEmpty()
        assertThat(repo.stories.first().map { it.story.title }).containsExactly("Của tôi")
    }

    @Test
    fun messagesGetSequentialPositionsAndNarrationHasNoSpeaker() = runBlocking {
        val a = repo.saveCharacter(null, "A", 0xFF000000, false, null)
        val story = repo.createStory("S", ThemePreset.LYSN_BUBBLE)
        repo.addMessage(story, a, MessageType.TEXT, "một")
        repo.addMessage(story, a, MessageType.NARRATION, "dẫn")
        repo.addMessage(story, a, MessageType.VOICE, voiceSeconds = 9)
        val messages = repo.messages(story).first()
        assertThat(messages.map { it.position }).containsExactly(0, 1, 2).inOrder()
        assertThat(messages[1].characterId).isNull()
        assertThat(messages[2].voiceSeconds).isEqualTo(9)
    }

    @Test
    fun moveMessageReordersAndKeepsPositionsDense() = runBlocking {
        val a = repo.saveCharacter(null, "A", 0xFF000000, false, null)
        val story = repo.createStory("S", ThemePreset.LYSN_BUBBLE)
        listOf("1", "2", "3", "4").forEach { repo.addMessage(story, a, MessageType.TEXT, it) }

        repo.moveMessage(story, 3, 0)
        assertThat(repo.messages(story).first().map { it.text }).containsExactly("4", "1", "2", "3").inOrder()

        repo.moveMessage(story, 1, 2)
        val after = repo.messages(story).first()
        assertThat(after.map { it.text }).containsExactly("4", "2", "1", "3").inOrder()
        assertThat(after.map { it.position }).containsExactly(0, 1, 2, 3).inOrder()

        repo.moveMessage(story, 0, 99) // out of range: no-op
        assertThat(repo.messages(story).first().map { it.text }).containsExactly("4", "2", "1", "3").inOrder()
    }

    @Test
    fun onlyOneCharacterCanBeTheUser() = runBlocking {
        val a = repo.saveCharacter(null, "A", 1, true, null)
        val b = repo.saveCharacter(null, "B", 2, true, null)
        val all = repo.characters.first()
        assertThat(all.single { it.isUser }.id).isEqualTo(b)
        assertThat(all.first { it.id == a }.isUser).isFalse()
    }

    @Test
    fun replacingAvatarDeletesTheOldFile() = runBlocking {
        val id = repo.saveCharacter(null, "A", 1, false, "/files/old.webp")
        val existing = repo.characters.first().single { it.id == id }
        repo.saveCharacter(existing, "A2", 3, false, "/files/new.webp")
        assertThat(images.deleted).containsExactly("/files/old.webp")
        val updated = repo.characters.first().single()
        assertThat(updated.name).isEqualTo("A2")
        assertThat(updated.avatarPath).isEqualTo("/files/new.webp")
    }

    @Test
    fun deletingCharacterCascadesMessagesAndCleansFiles() = runBlocking {
        val a = repo.saveCharacter(null, "A", 1, false, "/files/a.webp")
        val b = repo.saveCharacter(null, "B", 2, true, null)
        val story = repo.createStory("S", ThemePreset.LYSN_BUBBLE)
        repo.addMessage(story, a, MessageType.IMAGE, imagePath = "/files/pic.webp")
        repo.addMessage(story, b, MessageType.TEXT, "giữ lại")
        repo.addMessage(story, null, MessageType.NARRATION, "dẫn")

        repo.deleteCharacter(repo.characters.first().first { it.id == a })

        assertThat(repo.messages(story).first().map { it.text }).containsExactly("giữ lại", "dẫn").inOrder()
        assertThat(images.deleted).containsExactly("/files/pic.webp", "/files/a.webp")
    }

    @Test
    fun deletingStoryRemovesMessagesAndImages() = runBlocking {
        val a = repo.saveCharacter(null, "A", 1, false, null)
        val story = repo.createStory("S", ThemePreset.LYSN_BUBBLE)
        repo.addMessage(story, a, MessageType.IMAGE, imagePath = "/files/p.webp")
        repo.deleteStory(story)
        assertThat(repo.stories.first()).isEmpty()
        assertThat(db.messageDao().getForStory(story)).isEmpty()
        assertThat(images.deleted).containsExactly("/files/p.webp")
    }

    @Test
    fun summariesAreNewestFirstWithLastMessagePreview() = runBlocking {
        val a = repo.saveCharacter(null, "A", 1, false, null)
        val old = repo.createStory("Cũ", ThemePreset.LYSN_BUBBLE)
        val fresh = repo.createStory("Mới", ThemePreset.INSTAGRAM_DM)
        repo.addMessage(old, a, MessageType.TEXT, "xin chào") // touching makes it newest
        val summaries = repo.stories.first()
        assertThat(summaries.map { it.story.id }).containsExactly(old, fresh).inOrder()
        assertThat(summaries[0].lastText).isEqualTo("xin chào")
        assertThat(summaries[1].messageCount).isEqualTo(0)
        assertThat(summaries[1].lastText).isNull()
    }

    @Test
    fun themeRenameAndChatTitleArePersisted() = runBlocking {
        val story = repo.createStory("S", ThemePreset.LYSN_BUBBLE)
        repo.setTheme(story, ThemePreset.WEVERSE_DM)
        repo.renameStory(story, "Tên mới")
        repo.setChatTitle(story, "Minho 🐰")
        val saved = repo.story(story).first()!!
        assertThat(saved.theme).isEqualTo(ThemePreset.WEVERSE_DM)
        assertThat(saved.title).isEqualTo("Tên mới")
        assertThat(saved.chatTitle).isEqualTo("Minho 🐰")
    }

    @Test
    fun deleteMessageRemovesItsImage() = runBlocking {
        val a = repo.saveCharacter(null, "A", 1, false, null)
        val story = repo.createStory("S", ThemePreset.LYSN_BUBBLE)
        repo.addMessage(story, a, MessageType.IMAGE, imagePath = "/files/x.webp")
        val msg = repo.messages(story).first().single()
        repo.deleteMessage(msg)
        assertThat(repo.messages(story).first()).isEmpty()
        assertThat(images.deleted).containsExactly("/files/x.webp")
    }
}
