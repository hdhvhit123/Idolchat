package com.fandom.auchatstudio.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fandom.auchatstudio.core.MessageType
import com.fandom.auchatstudio.core.TapToChatState
import com.fandom.auchatstudio.core.ThemePreset
import com.fandom.auchatstudio.data.CharacterEntity
import com.fandom.auchatstudio.data.MessageEntity
import com.fandom.auchatstudio.data.StoryEntity
import com.fandom.auchatstudio.data.StorySummary
import com.fandom.auchatstudio.ui.chat.ChatLayout
import com.fandom.auchatstudio.ui.home.HomeContent
import com.fandom.auchatstudio.ui.home.HomeUiState
import com.fandom.auchatstudio.ui.player.PlayerContent
import com.fandom.auchatstudio.ui.player.PlayerUiState
import com.fandom.auchatstudio.ui.theme.AUChatStudioTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class ComposeScreensTest {
    @get:Rule
    val rule = createComposeRule()

    private val idol = CharacterEntity(id = 1, name = "Minho", roleColorHex = 0xFFFF6584)

    @Test
    fun homeShowsEmptyStateAndCreatesStoryWithChosenTheme() {
        var created: Pair<String, ThemePreset>? = null
        rule.setContent {
            AUChatStudioTheme {
                HomeContent(
                    state = HomeUiState(loading = false),
                    onOpenStory = {},
                    onPlayStory = {},
                    onOpenCharacters = {},
                    onCreateStory = { t, p -> created = t to p },
                    onRenameStory = { _, _ -> },
                    onDeleteStory = {},
                )
            }
        }
        rule.onNodeWithText("Chưa có kịch bản nào").assertIsDisplayed()
        rule.onNodeWithTag("fab_new_story").performClick()
        rule.onNodeWithTag("new_story_title").performTextInput("Concert")
        rule.onNodeWithText("Instagram DM").performScrollTo().performClick()
        rule.onNodeWithTag("new_story_confirm").performClick()
        assertThat(created).isEqualTo("Concert" to ThemePreset.INSTAGRAM_DM)
    }

    @Test
    fun homeListsStoriesWithPreview() {
        var opened = -1L
        val summary = StorySummary(
            story = StoryEntity(id = 7, title = "Hẹn hò bí mật", theme = ThemePreset.WEVERSE_DM),
            messageCount = 3,
            lastText = null,
            lastType = MessageType.VOICE,
        )
        rule.setContent {
            AUChatStudioTheme {
                HomeContent(
                    state = HomeUiState(loading = false, stories = listOf(summary), characters = listOf(idol)),
                    onOpenStory = { opened = it },
                    onPlayStory = {},
                    onOpenCharacters = {},
                    onCreateStory = { _, _ -> },
                    onRenameStory = { _, _ -> },
                    onDeleteStory = {},
                )
            }
        }
        rule.onNodeWithText("🎙 Tin nhắn thoại").assertIsDisplayed()
        rule.onNodeWithText("Weverse DM · 3 tin nhắn").assertIsDisplayed()
        rule.onNodeWithText("Hẹn hò bí mật").performClick()
        assertThat(opened).isEqualTo(7L)
    }

    @Test
    fun playerForwardsTapsAndShowsEnding() {
        var taps = 0
        var restarts = 0
        val msg = MessageEntity(id = 1, storyId = 1, characterId = 1, text = "Còn thức không?", position = 0)
        val playing = PlayerUiState(
            loading = false,
            story = StoryEntity(id = 1, title = "S", theme = ThemePreset.LYSN_BUBBLE),
            headerTitle = "Minho",
            partners = listOf(idol),
            visible = ChatLayout.build(listOf(msg), listOf(idol)),
            playback = TapToChatState(total = 2, revealed = 1),
        )
        var state by mutableStateOf(playing)
        rule.setContent {
            AUChatStudioTheme {
                PlayerContent(state = state, onTap = { taps++ }, onRestart = { restarts++ }, onAutoPlay = {}, onBack = {})
            }
        }
        rule.onNodeWithText("Còn thức không?").assertIsDisplayed()
        rule.onNodeWithTag("player_surface").performClick()
        assertThat(taps).isEqualTo(1)

        state = playing.copy(playback = TapToChatState(total = 2, revealed = 2))
        rule.onNodeWithTag("player_finished").assertIsDisplayed()
        rule.onNodeWithTag("player_restart").performClick()
        assertThat(restarts).isEqualTo(1)
    }
}
