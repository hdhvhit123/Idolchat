package com.fandom.auchatstudio.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fandom.auchatstudio.core.MessageType
import com.fandom.auchatstudio.core.StoryTemplate
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
import com.fandom.auchatstudio.ui.studio.Speaker
import com.fandom.auchatstudio.ui.studio.StudioContent
import com.fandom.auchatstudio.ui.studio.StudioUiState
import com.fandom.auchatstudio.ui.theme.AUChatStudioTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Screens are checked against the Vietnamese texts in values-vi. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "vi")
class ComposeScreensTest {
    @get:Rule
    val rule = createComposeRule()

    private val idol = CharacterEntity(id = 1, name = "Minho", roleColorHex = 0xFFFF6584)
    private val sent = mutableListOf<String>()

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
        rule.onNodeWithText("Weverse DM").performClick()
        rule.onNodeWithTag("new_story_confirm").performClick()
        assertThat(created).isEqualTo("Concert" to ThemePreset.WEVERSE_DM)
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
    fun homeOffersGuideTemplatesAndStoryMenuActions() {
        var guideDismissed = false
        var template: StoryTemplate? = null
        var duplicated = -1L
        val summary = StorySummary(
            story = StoryEntity(id = 7, title = "Hẹn hò bí mật", theme = ThemePreset.LYSN_BUBBLE),
            messageCount = 0,
            lastText = null,
            lastType = null,
        )
        rule.setContent {
            AUChatStudioTheme {
                HomeContent(
                    state = HomeUiState(loading = false, stories = listOf(summary), characters = listOf(idol), showGuide = true),
                    onOpenStory = {},
                    onPlayStory = {},
                    onOpenCharacters = {},
                    onCreateStory = { _, _ -> },
                    onRenameStory = { _, _ -> },
                    onDeleteStory = {},
                    onUseTemplate = { template = it },
                    onDuplicateStory = { duplicated = it },
                    onDismissGuide = { guideDismissed = true },
                )
            }
        }
        // An empty story has nothing to play, so no "Phát" button is offered.
        rule.onNodeWithText("Chưa có tin nhắn").assertIsDisplayed()
        rule.onNodeWithText("Phát").assertDoesNotExist()

        rule.onNodeWithContentDescription("Tùy chọn Hẹn hò bí mật").performClick()
        rule.onNodeWithText("Nhân bản").performClick()
        assertThat(duplicated).isEqualTo(7L)

        rule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("guide_dismiss"))
        rule.onNodeWithTag("guide_dismiss").performClick()
        assertThat(guideDismissed).isTrue()

        rule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("template_good_night"))
        rule.onNodeWithTag("template_good_night").performClick()
        assertThat(template?.id).isEqualTo("good_night")
    }

    @Test
    fun homeShowsLoaderInsteadOfEmptyStateWhileStartingUp() {
        rule.setContent {
            AUChatStudioTheme {
                HomeContent(
                    state = HomeUiState(loading = true),
                    onOpenStory = {},
                    onPlayStory = {},
                    onOpenCharacters = {},
                    onCreateStory = { _, _ -> },
                    onRenameStory = { _, _ -> },
                    onDeleteStory = {},
                )
            }
        }
        rule.onNodeWithText("Chưa có kịch bản nào").assertDoesNotExist()
        rule.onNodeWithTag("fab_new_story").assertIsDisplayed()
    }

    @Test
    fun studioHeaderCarriesStoryTitleSkinAndActions() {
        var cycled = 0
        var played = 0
        val msg = MessageEntity(id = 1, storyId = 1, characterId = 1, text = "Còn thức không?", position = 0)
        rule.setContent {
            AUChatStudioTheme {
                StudioContent(
                    state = StudioUiState(
                        loading = false,
                        story = StoryEntity(id = 1, title = "Nửa đêm", theme = ThemePreset.WEVERSE_DM),
                        characters = listOf(idol),
                        items = ChatLayout.build(listOf(msg), listOf(idol)),
                        headerTitle = "Minho",
                        partners = listOf(idol),
                        speaker = Speaker.Narrator,
                    ),
                    snackbar = remember { SnackbarHostState() },
                    onBack = {},
                    onPlay = { played++ },
                    onOpenCharacters = {},
                    onCycleTheme = { cycled++ },
                    onExport = {},
                    onRename = {},
                    onSetChatTitle = {},
                    onSelectSpeaker = {},
                    onSendText = { sent += it; true },
                    onSendVoice = {},
                    onSendImage = {},
                    onUpdateMessage = { _, _, _, _ -> },
                    onChangeSpeaker = { _, _ -> },
                    onMove = { _, _ -> },
                    onDelete = {},
                )
            }
        }
        rule.onNodeWithTag("studio_story_title", useUnmergedTree = true).assertTextEquals("Nửa đêm")
        rule.onNodeWithTag("studio_theme_label", useUnmergedTree = true).assertTextEquals("Weverse DM")
        rule.onNodeWithTag("cycle_theme").performClick()
        rule.onNodeWithTag("play_story").performClick()
        assertThat(cycled).isEqualTo(1)
        assertThat(played).isEqualTo(1)

        // With the narrator selected, one tap on a preset sends that stage direction.
        rule.onNodeWithTag("narration_presets").assertIsDisplayed()
        rule.onNodeWithText("Đã xem").performClick()
        assertThat(sent).containsExactly("Đã xem")
    }

    @Test
    fun playerShowsStartHintOnlyBeforeTheFirstTap() {
        val waiting = PlayerUiState(
            loading = false,
            story = StoryEntity(id = 1, title = "S", theme = ThemePreset.INSTAGRAM_DM),
            headerTitle = "Minho",
            partners = listOf(idol),
            playback = TapToChatState(total = 3, revealed = 0),
        )
        var state by mutableStateOf(waiting)
        rule.setContent {
            AUChatStudioTheme {
                PlayerContent(state = state, onTap = {}, onRestart = {}, onAutoPlay = {}, onBack = {})
            }
        }
        rule.onNodeWithText("Chạm để bắt đầu").assertIsDisplayed()
        rule.onNodeWithText("3 tin nhắn · mỗi lần chạm hiện một tin").assertIsDisplayed()

        val msg = MessageEntity(id = 1, storyId = 1, characterId = 1, text = "Hi", position = 0)
        state = waiting.copy(
            visible = ChatLayout.build(listOf(msg), listOf(idol)),
            playback = TapToChatState(total = 3, revealed = 1),
        )
        rule.waitForIdle()
        rule.onNodeWithText("Chạm để bắt đầu").assertDoesNotExist()
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
