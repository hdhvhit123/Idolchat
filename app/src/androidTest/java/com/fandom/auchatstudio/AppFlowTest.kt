package com.fandom.auchatstudio

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fandom.auchatstudio.core.MessageType
import com.fandom.auchatstudio.core.ThemePreset
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

        rule.onNodeWithContentDescription("Người nói: Dẫn truyện").performClick()
        rule.onNodeWithTag("composer_input").performTextInput("Ba ngày sau")
        rule.onNodeWithTag("composer_send").performClick()
        rule.waitUntilAtLeastOneExists(hasText("Ba ngày sau"), 5_000)

        rule.onNodeWithText("Studio · Lysn Bubble").assertIsDisplayed()
        rule.onNodeWithTag("cycle_theme").performClick()
        rule.waitUntilAtLeastOneExists(hasText("Studio · Weverse DM"), 5_000)
        rule.onNodeWithTag("cycle_theme").performClick()
        rule.waitUntilAtLeastOneExists(hasText("Studio · Instagram DM"), 5_000)
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
        rule.onNodeWithText("Weverse DM").performScrollTo().performClick()
        rule.onNodeWithTag("new_story_confirm").performClick()
        rule.waitUntilAtLeastOneExists(hasText("Studio · Weverse DM"), 10_000)
        rule.onNodeWithText(newTitle).assertIsDisplayed()
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

    private fun androidx.compose.ui.test.junit4.ComposeTestRule.onAllNodesWithTagExists(tag: String): Boolean =
        onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
}
