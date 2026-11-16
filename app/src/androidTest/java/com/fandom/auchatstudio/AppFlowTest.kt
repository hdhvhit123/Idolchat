package com.fandom.auchatstudio

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fandom.auchatstudio.core.MessageType
import com.fandom.auchatstudio.core.ThemePreset
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * End-to-end flows on a real device/emulator. Each test creates its own uniquely named story so
 * tests stay independent of each other and of the seeded sample data.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class AppFlowTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val app: AuChatApp
        get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as AuChatApp

    private lateinit var title: String

    @Before
    fun setUp() {
        title = "UI test ${UUID.randomUUID().toString().take(6)}"
        runBlocking {
            val repo = app.container.repository
            repo.seedIfEmpty()
            val cast = repo.characters.first()
            val idol = cast.first { !it.isUser }
            val me = cast.first { it.isUser }
            val story = repo.createStory(title, ThemePreset.LYSN_BUBBLE)
            repo.addMessage(story, idol.id, MessageType.TEXT, "Xin chào từ test")
            repo.addMessage(story, null, MessageType.NARRATION, "Một lúc sau")
            repo.addMessage(story, me.id, MessageType.TEXT, "Chào anh")
        }
    }

    private fun openOwnStory() {
        rule.waitUntilAtLeastOneExists(hasText(title), 10_000)
        rule.onNodeWithText(title).performClick()
        rule.waitUntilAtLeastOneExists(hasText("Xin chào từ test"), 10_000)
    }

    @Test
    fun homeShowsHeroAndStories() {
        rule.onNodeWithText("AU Chat Studio").assertIsDisplayed()
        rule.waitUntilAtLeastOneExists(hasText(title), 10_000)
        rule.onNodeWithText(title).assertIsDisplayed()
    }

    @Test
    fun composeMessagesAsDifferentSpeakersAndSwitchTheme() {
        openOwnStory()
        rule.onNodeWithTag("composer_input").performTextInput("Tin nhắn mới nè")
        rule.onNodeWithTag("composer_send").performClick()
        rule.waitUntilAtLeastOneExists(hasText("Tin nhắn mới nè"), 5_000)

        // With a long cast the narrator chip is off screen and not composed yet.
        rule.onNodeWithTag("director_switcher").performScrollToNode(hasContentDescription("Người nói: Dẫn truyện"))
        rule.onNodeWithContentDescription("Người nói: Dẫn truyện").performClick()
        rule.onNodeWithTag("composer_input").performTextInput("Ba ngày sau")
        rule.onNodeWithTag("composer_send").performClick()
        rule.waitUntilAtLeastOneExists(hasText("Ba ngày sau"), 5_000)

        waitForTheme("Lysn Bubble")
        rule.onNodeWithTag("cycle_theme").performClick()
        waitForTheme("Weverse DM")
        rule.onNodeWithTag("cycle_theme").performClick()
        waitForTheme("Instagram DM")
    }

    @Test
    fun narratorPresetSendsInOneTap() {
        openOwnStory()
        // With a long cast the narrator chip is off screen and not composed yet.
        rule.onNodeWithTag("director_switcher").performScrollToNode(hasContentDescription("Người nói: Dẫn truyện"))
        rule.onNodeWithContentDescription("Người nói: Dẫn truyện").performClick()
        rule.waitUntilAtLeastOneExists(hasTestTag("narration_presets"), 5_000)
        rule.onNodeWithText("Hôm sau").performClick()
        rule.waitUntil(5_000) {
            val messages = runBlocking { app.container.repository.stories.first() }
                .first { it.story.title == title }
            messages.messageCount == 4 && messages.lastText == "Hôm sau"
        }
    }

    @Test
    fun editAndDeleteMessage() {
        openOwnStory()
        rule.onNodeWithText("Chào anh").performClick()
        rule.waitUntilAtLeastOneExists(hasText("Sửa nội dung & giờ"), 5_000)
        rule.onNodeWithText("Sửa nội dung & giờ").performClick()
        rule.onNodeWithTag("edit_message_text").performTextReplacement("Chào anh nha")
        rule.onNodeWithTag("edit_message_save").performClick()
        rule.waitUntilAtLeastOneExists(hasText("Chào anh nha"), 5_000)

        rule.onNodeWithText("Chào anh nha").performClick()
        rule.waitUntilAtLeastOneExists(hasTestTag("delete_message"), 5_000)
        rule.onNodeWithTag("delete_message").performClick()
        rule.waitUntilDoesNotExist(hasText("Chào anh nha"), 5_000)

        // The delete can be taken back from the snackbar.
        rule.waitUntilAtLeastOneExists(hasText("Hoàn tác"), 5_000)
        rule.onNodeWithText("Hoàn tác").performClick()
        rule.waitUntilAtLeastOneExists(hasText("Chào anh nha"), 5_000)
    }

    @Test
    fun tapToChatPlaysWholeScript() {
        openOwnStory()
        rule.onNodeWithTag("play_story").performClick()
        rule.waitUntilAtLeastOneExists(hasTestTag("player_surface"), 5_000)
        // 3 messages: two need typing (tap to start + tap to skip), narration is instant.
        repeat(8) {
            if (rule.onAllNodesWithTagExists("player_finished")) return@repeat
            rule.onNodeWithTag("player_surface").performClick()
            rule.waitForIdle()
        }
        rule.waitUntilAtLeastOneExists(hasTestTag("player_finished"), 10_000)
        rule.onNodeWithText("Xin chào từ test").assertIsDisplayed()
        rule.onNodeWithTag("player_restart").performClick()
        rule.waitUntilDoesNotExist(hasText("Chào anh"), 5_000)
    }

    @Test
    fun createStoryFromHome() {
        val newTitle = "Mới ${UUID.randomUUID().toString().take(4)}"
        rule.onNodeWithTag("fab_new_story").performClick()
        rule.onNodeWithTag("new_story_title").performTextInput(newTitle)
        rule.onNodeWithText("Weverse DM").performClick()
        rule.onNodeWithTag("new_story_confirm").performClick()
        waitForTheme("Weverse DM")
        rule.onNodeWithTag("studio_story_title", useUnmergedTree = true).assertTextEquals(newTitle)
    }

    @Test
    fun templateCreatesReadyMadeStoryAndOpensIt() {
        rule.waitUntilAtLeastOneExists(hasText(title), 10_000)
        rule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("template_good_night"))
        rule.onNodeWithTag("template_good_night").performClick()
        waitForTheme("Lysn Bubble")
        rule.onNodeWithTag("studio_story_title", useUnmergedTree = true).assertTextEquals("Chúc ngủ ngon")
        rule.waitUntilAtLeastOneExists(hasText("Ngủ chưa đó?"), 5_000)
        // A template story is a normal story: it can be played straight away.
        rule.onNodeWithTag("play_story").assertIsEnabled()
    }

    @Test
    fun duplicateStoryFromHomeMenu() {
        rule.waitUntilAtLeastOneExists(hasText(title), 10_000)
        rule.onNodeWithContentDescription("Tùy chọn $title").performClick()
        rule.onNodeWithText("Nhân bản").performClick()
        rule.waitUntilAtLeastOneExists(hasText("$title (bản sao)"), 10_000)
        val summaries = runBlocking { app.container.repository.stories.first() }
        assertThat(summaries.first { it.story.title == "$title (bản sao)" }.messageCount).isEqualTo(3)
        assertThat(summaries.first { it.story.title == title }.messageCount).isEqualTo(3)
    }

    @Test
    fun addCharacter() {
        val name = "Idol ${UUID.randomUUID().toString().take(4)}"
        rule.onNodeWithContentDescription("Nhân vật").performClick()
        rule.waitUntilAtLeastOneExists(hasTestTag("fab_add_character"), 5_000)
        rule.onNodeWithTag("fab_add_character").performClick()
        rule.onNodeWithTag("character_name_field").performTextInput(name)
        rule.onNodeWithTag("character_save").performClick()
        rule.waitUntilAtLeastOneExists(hasContentDescription("Sửa $name"), 5_000)
    }

    /** The skin name lives inside the tappable header, so it is only visible in the unmerged tree. */
    private fun waitForTheme(name: String) {
        rule.waitUntil(10_000) {
            rule.onAllNodes(hasTestTag("studio_theme_label") and hasText(name), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun androidx.compose.ui.test.junit4.ComposeTestRule.onAllNodesWithTagExists(tag: String): Boolean =
        onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
}
