package com.fandom.auchatstudio

import android.graphics.Bitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
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
 * Run it on its own (class filter) so the app starts from the seeded sample data. Words are
 * looked up in the app's resources, so the tour runs in the device's language.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class ScreenshotTourTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext

    /** The sample is seeded in the device's language on first launch. */
    private val sampleTitle: String get() = context.getString(R.string.sample_title)
    private val sampleLine: String get() = context.resources.getStringArray(R.array.sample_lines)[1]
    private val narratorChip: String get() = context.getString(R.string.cd_speaker, context.getString(R.string.narrator))
    private val charactersButton: String get() = context.getString(R.string.cd_characters)

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
        // A system "app isn't responding" dialog on a slow emulator would take window focus,
        // which Espresso's pressBack needs, and would end up in the screenshots.
        instrumentation.uiAutomation
            .executeShellCommand("am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS").close()
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

    private fun waitForHome() = rule.waitUntilAtLeastOneExists(hasTestTag("home_list"), 10_000)

    /**
     * The sample is the "continue" card on a fresh install, but when other tests ran first it sits
     * further down the lazy list and is not composed until scrolled to.
     */
    private fun openSampleStory() {
        waitForHome()
        rule.onNodeWithTag("home_list").performScrollToNode(hasText(sampleTitle))
        rule.onNodeWithText(sampleTitle).performClick()
        rule.waitUntilAtLeastOneExists(hasText(sampleLine), 10_000)
    }

    @Test
    fun lightTour() {
        waitForHome()
        shot("01_home")
        rule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("cast_strip"))
        shot("01b_home_scrolled")

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

        rule.onNodeWithText(sampleLine).performClick()
        rule.waitUntilAtLeastOneExists(hasTestTag("delete_message"), 5_000)
        shot("06_message_actions")
        Espresso.pressBack()

        rule.onNodeWithTag("composer_input").performClick()
        shot("07_studio_keyboard")
        Espresso.closeSoftKeyboard()

        // With a long cast the narrator chip is off screen and not composed yet.
        rule.onNodeWithTag("director_switcher").performScrollToNode(hasContentDescription(narratorChip))
        rule.onNodeWithContentDescription(narratorChip).performClick()
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
        rule.onNodeWithContentDescription(charactersButton).performClick()
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
            waitForHome()
            shot("20_home_dark")
            openSampleStory()
            shot("21_studio_dark")
            Espresso.pressBack()
            rule.waitUntilAtLeastOneExists(hasTestTag("fab_new_story"), 5_000)
            rule.onNodeWithContentDescription(charactersButton).performClick()
            rule.waitUntilAtLeastOneExists(hasTestTag("fab_add_character"), 5_000)
            shot("22_characters_dark")
        } finally {
            shell("cmd uimode night no")
        }
    }
}
