package com.fandom.auchatstudio

import android.graphics.Bitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * Walks the main journeys and saves a full-screen PNG of each step. AGP copies the files to
 * build/outputs/connected_android_test_additional_output so CI can publish them for review.
 * Run it on its own (class filter) so the app starts from the seeded sample data.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class ScreenshotTourTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    private val outputDir: File by lazy {
        val fromAgp = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
        val dir = if (fromAgp != null) {
            File(fromAgp)
        } else {
            File(instrumentation.targetContext.externalMediaDirs.first(), "additional_test_output")
        }
        dir.apply { mkdirs() }
    }

    @Before
    fun seed() {
        val app = instrumentation.targetContext.applicationContext as AuChatApp
        runBlocking { app.container.repository.seedIfEmpty() }
    }

    private fun shot(name: String) {
        rule.waitForIdle()
        // The CI emulator renders in software and presents frames late; give it time to catch up.
        Thread.sleep(1_500)
        val bitmap: Bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
        FileOutputStream(File(outputDir, "$name.png")).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun shell(command: String) {
        instrumentation.uiAutomation.executeShellCommand(command).close()
        Thread.sleep(1_500)
    }

    private fun openSampleStory() {
        rule.waitUntilAtLeastOneExists(hasText(SAMPLE_TITLE), 10_000)
        rule.onNodeWithText(SAMPLE_TITLE).performClick()
        rule.waitUntilAtLeastOneExists(hasText("Còn thức không đó?"), 10_000)
    }

    @Test
    fun lightTour() {
        rule.waitUntilAtLeastOneExists(hasText(SAMPLE_TITLE), 10_000)
        shot("01_home")
        rule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("cast_strip"))
        shot("01b_home_scrolled")
        rule.onNodeWithTag("home_list").performScrollToNode(hasText(SAMPLE_TITLE))

        rule.onNodeWithTag("fab_new_story").performClick()
        shot("02_new_story")
        Espresso.pressBack()

        openSampleStory()
        shot("03_studio_lysn")
        rule.onNodeWithTag("cycle_theme").performClick()
        shot("04_studio_weverse")
        rule.onNodeWithTag("cycle_theme").performClick()
        shot("05_studio_instagram")
        rule.onNodeWithTag("cycle_theme").performClick()

        rule.onNodeWithText("Còn thức không đó?").performClick()
        rule.waitUntilAtLeastOneExists(hasTestTag("delete_message"), 5_000)
        shot("06_message_actions")
        Espresso.pressBack()

        rule.onNodeWithTag("composer_input").performClick()
        shot("07_studio_keyboard")
        Espresso.closeSoftKeyboard()

        rule.onNodeWithContentDescription("Người nói: Dẫn truyện").performClick()
        rule.waitUntilAtLeastOneExists(hasTestTag("narration_presets"), 5_000)
        shot("07b_studio_narrator")

        rule.onNodeWithTag("play_story").performClick()
        rule.waitUntilAtLeastOneExists(hasTestTag("player_surface"), 5_000)
        shot("08_player_start")
        repeat(3) {
            rule.onNodeWithTag("player_surface").performClick()
            rule.waitForIdle()
        }
        shot("09_player_typing")
        repeat(30) {
            if (rule.onAllNodes(hasTestTag("player_finished")).fetchSemanticsNodes().isEmpty()) {
                rule.onNodeWithTag("player_surface").performClick()
                rule.waitForIdle()
            }
        }
        shot("10_player_end")
        Espresso.pressBack()
        Espresso.pressBack()

        rule.waitUntilAtLeastOneExists(hasTestTag("fab_new_story"), 5_000)
        rule.onNodeWithContentDescription("Nhân vật").performClick()
        rule.waitUntilAtLeastOneExists(hasTestTag("fab_add_character"), 5_000)
        shot("11_characters")
        rule.onNodeWithText("Hyunjin").performClick()
        rule.waitUntilAtLeastOneExists(hasTestTag("character_save"), 5_000)
        shot("12_edit_character")
    }

    @Test
    fun darkTour() {
        shell("cmd uimode night yes")
        try {
            rule.waitUntilAtLeastOneExists(hasText(SAMPLE_TITLE), 10_000)
            shot("20_home_dark")
            openSampleStory()
            shot("21_studio_dark")
            Espresso.pressBack()
            rule.waitUntilAtLeastOneExists(hasTestTag("fab_new_story"), 5_000)
            rule.onNodeWithContentDescription("Nhân vật").performClick()
            rule.waitUntilAtLeastOneExists(hasTestTag("fab_add_character"), 5_000)
            shot("22_characters_dark")
        } finally {
            shell("cmd uimode night no")
        }
    }

    private companion object {
        const val SAMPLE_TITLE = "Tin nhắn lúc nửa đêm"
    }
}
