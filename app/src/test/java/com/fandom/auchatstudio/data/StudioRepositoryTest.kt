package com.fandom.auchatstudio.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fandom.auchatstudio.FakeImageStore
import com.fandom.auchatstudio.core.MessageType
import com.fandom.auchatstudio.core.StoryTemplates
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
    fun seedCreatesSampleCastAndStoryOnlyOnce() = runBlocking<Unit> {
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
    fun seedSkipsWhenUserAlreadyHasData() = runBlocking<Unit> {
        repo.createStory("Của tôi", ThemePreset.WEVERSE_DM)
        repo.seedIfEmpty()
        assertThat(repo.characters.first()).isEmpty()
        assertThat(repo.stories.first().map { it.story.title }).containsExactly("Của tôi")
    }

    @Test
    fun messagesGetSequentialPositionsAndNarrationHasNoSpeaker() = runBlocking<Unit> {
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
    fun moveMessageReordersAndKeepsPositionsDense() = runBlocking<Unit> {
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
    fun onlyOneCharacterCanBeTheUser() = runBlocking<Unit> {
        val a = repo.saveCharacter(null, "A", 1, true, null)
        val b = repo.saveCharacter(null, "B", 2, true, null)
        val all = repo.characters.first()
        assertThat(all.single { it.isUser }.id).isEqualTo(b)
        assertThat(all.first { it.id == a }.isUser).isFalse()
    }

    @Test
    fun replacingAvatarDeletesTheOldFile() = runBlocking<Unit> {
        val id = repo.saveCharacter(null, "A", 1, false, "/files/old.webp")
        val existing = repo.characters.first().single { it.id == id }
        repo.saveCharacter(existing, "A2", 3, false, "/files/new.webp")
        assertThat(images.deleted).containsExactly("/files/old.webp")
        val updated = repo.characters.first().single()
        assertThat(updated.name).isEqualTo("A2")
        assertThat(updated.avatarPath).isEqualTo("/files/new.webp")
    }

    @Test
    fun deletingCharacterCascadesMessagesAndCleansFiles() = runBlocking<Unit> {
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
    fun deletingStoryRemovesMessagesAndImages() = runBlocking<Unit> {
        val a = repo.saveCharacter(null, "A", 1, false, null)
        val story = repo.createStory("S", ThemePreset.LYSN_BUBBLE)
        repo.addMessage(story, a, MessageType.IMAGE, imagePath = "/files/p.webp")
        repo.deleteStory(story)
        assertThat(repo.stories.first()).isEmpty()
        assertThat(db.messageDao().getForStory(story)).isEmpty()
        assertThat(images.deleted).containsExactly("/files/p.webp")
    }

    @Test
    fun summariesAreNewestFirstWithLastMessagePreview() = runBlocking<Unit> {
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
    fun themeRenameAndChatTitleArePersisted() = runBlocking<Unit> {
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
    fun deleteMessageRemovesItsImage() = runBlocking<Unit> {
        val a = repo.saveCharacter(null, "A", 1, false, null)
        val story = repo.createStory("S", ThemePreset.LYSN_BUBBLE)
        repo.addMessage(story, a, MessageType.IMAGE, imagePath = "/files/x.webp")
        val msg = repo.messages(story).first().single()
        repo.deleteMessage(msg)
        assertThat(repo.messages(story).first()).isEmpty()
        assertThat(images.deleted).containsExactly("/files/x.webp")
    }

    @Test
    fun initializeSeedsOnlyWhenAskedAndThenReportsReady() = runBlocking<Unit> {
        assertThat(repo.ready.value).isFalse()
        repo.initialize(seedSample = false)
        assertThat(repo.ready.value).isTrue()
        assertThat(repo.stories.first()).isEmpty()

        repo.initialize(seedSample = true)
        assertThat(repo.stories.first()).hasSize(1)
    }

    @Test
    fun duplicateStoryCopiesMessagesAndGivesImagesTheirOwnFiles() = runBlocking<Unit> {
        val a = repo.saveCharacter(null, "A", 0xFF000000, false, null)
        val story = repo.createStory("Gốc", ThemePreset.WEVERSE_DM)
        repo.setChatTitle(story, "Nhóm chat")
        repo.addMessage(story, a, MessageType.TEXT, "một", timeLabel = "23:41")
        repo.addMessage(story, a, MessageType.IMAGE, imagePath = "/files/p.webp")
        repo.addMessage(story, null, MessageType.NARRATION, "hết")

        val copyId = repo.duplicateStory(story)!!

        val copy = repo.story(copyId).first()!!
        assertThat(copy.title).isEqualTo("Gốc (bản sao)")
        assertThat(copy.theme).isEqualTo(ThemePreset.WEVERSE_DM)
        assertThat(copy.chatTitle).isEqualTo("Nhóm chat")
        val copied = repo.messages(copyId).first()
        assertThat(copied.map { it.text }).containsExactly("một", "", "hết").inOrder()
        assertThat(copied[0].timeLabel).isEqualTo("23:41")
        assertThat(copied[1].imagePath).isEqualTo("/files/p.webp.copy0")
        assertThat(images.copied).containsExactly("/files/p.webp")
        // The original is untouched, and deleting the copy must not delete the original's file.
        assertThat(repo.messages(story).first()).hasSize(3)
        repo.deleteStory(copyId)
        assertThat(images.deleted).containsExactly("/files/p.webp.copy0")
    }

    @Test
    fun duplicateStoryDropsAnImageThatCannotBeCopiedAndHandlesMissingStory() = runBlocking<Unit> {
        val a = repo.saveCharacter(null, "A", 0xFF000000, false, null)
        val story = repo.createStory("Gốc", ThemePreset.LYSN_BUBBLE)
        repo.addMessage(story, a, MessageType.TEXT, "một")
        repo.addMessage(story, a, MessageType.IMAGE, imagePath = "/files/p.webp")
        images.failCopy = true

        val copyId = repo.duplicateStory(story)!!

        assertThat(repo.messages(copyId).first().map { it.type }).containsExactly(MessageType.TEXT)
        assertThat(repo.duplicateStory(9_999)).isNull()
    }

    @Test
    fun templateUsesExistingCastAndCreatesOnlyMissingRoles() = runBlocking<Unit> {
        val idol = repo.saveCharacter(null, "Minho", 0xFFFF6584, false, null)
        val template = StoryTemplates.byId("group_chat")!!

        val storyId = repo.createStoryFromTemplate(template)

        val cast = repo.characters.first()
        // The idol was reused; "me" and a second member were missing and got placeholders.
        assertThat(cast.map { it.name }).containsExactly("Minho", "Y/N (Bạn)", "Thành viên")
        assertThat(cast.count { it.isUser }).isEqualTo(1)
        assertThat(cast.map { it.roleColorHex }.distinct()).hasSize(3)
        val story = repo.story(storyId).first()!!
        assertThat(story.title).isEqualTo(template.title)
        assertThat(story.theme).isEqualTo(template.theme)
        val messages = repo.messages(storyId).first()
        assertThat(messages).hasSize(template.lines.size)
        assertThat(messages.map { it.position }).isEqualTo(template.lines.indices.toList())
        assertThat(messages.filter { it.type == MessageType.NARRATION }.all { it.characterId == null }).isTrue()
        assertThat(messages.mapNotNull { it.characterId }.toSet()).containsAtLeast(idol, cast[1].id, cast[2].id)
    }

    @Test
    fun everyBuiltInTemplateCanBeCreatedTwiceWithoutAddingCharactersAgain() = runBlocking<Unit> {
        StoryTemplates.all.forEach { repo.createStoryFromTemplate(it) }
        val castAfterFirst = repo.characters.first()
        StoryTemplates.all.forEach { repo.createStoryFromTemplate(it) }
        assertThat(repo.characters.first()).isEqualTo(castAfterFirst)
        assertThat(repo.stories.first()).hasSize(StoryTemplates.all.size * 2)
    }

    @Test
    fun deletedMessageCanBeRestoredToItsOldPlaceWithItsImage() = runBlocking<Unit> {
        val a = repo.saveCharacter(null, "A", 0xFF000000, false, null)
        val story = repo.createStory("S", ThemePreset.LYSN_BUBBLE)
        repo.addMessage(story, a, MessageType.TEXT, "một")
        repo.addMessage(story, a, MessageType.IMAGE, imagePath = "/files/x.webp")
        repo.addMessage(story, a, MessageType.TEXT, "ba")
        val (first, image, third) = repo.messages(story).first()

        repo.deleteMessage(image, deleteImage = false)
        assertThat(images.deleted).isEmpty()
        assertThat(repo.messages(story).first().map { it.text }).containsExactly("một", "ba").inOrder()

        assertThat(repo.restoreMessage(image)).isTrue()
        assertThat(repo.messages(story).first()).containsExactly(first, image, third).inOrder()
        // Restoring twice is harmless.
        assertThat(repo.restoreMessage(image)).isTrue()
        assertThat(repo.messages(story).first()).hasSize(3)
    }

    @Test
    fun restoreFailsCleanlyWhenStoryOrSpeakerIsGone() = runBlocking<Unit> {
        val a = repo.saveCharacter(null, "A", 0xFF000000, false, null)
        val b = repo.saveCharacter(null, "B", 0xFF111111, false, null)
        val story = repo.createStory("S", ThemePreset.LYSN_BUBBLE)
        repo.addMessage(story, a, MessageType.TEXT, "của A")
        repo.addMessage(story, b, MessageType.TEXT, "của B")
        val (fromA, fromB) = repo.messages(story).first()

        repo.deleteMessage(fromA, deleteImage = false)
        repo.deleteCharacter(db.characterDao().getById(a)!!)
        assertThat(repo.restoreMessage(fromA)).isFalse()

        repo.deleteMessage(fromB, deleteImage = false)
        repo.deleteStory(story)
        assertThat(repo.restoreMessage(fromB)).isFalse()
    }

    @Test
    fun orphanCleanupDeletesOnlyOldFilesNothingRefersTo() = runBlocking<Unit> {
        val a = repo.saveCharacter(null, "A", 0xFF000000, false, "/files/avatar.webp")
        val story = repo.createStory("S", ThemePreset.LYSN_BUBBLE)
        repo.addMessage(story, a, MessageType.IMAGE, imagePath = "/files/used.webp")
        images.stored = listOf("/files/avatar.webp", "/files/used.webp", "/files/orphan.webp")

        assertThat(repo.cleanupOrphanImages()).isEqualTo(1)
        assertThat(images.deleted).containsExactly("/files/orphan.webp")

        images.stored = emptyList()
        assertThat(repo.cleanupOrphanImages()).isEqualTo(0)
    }
}
