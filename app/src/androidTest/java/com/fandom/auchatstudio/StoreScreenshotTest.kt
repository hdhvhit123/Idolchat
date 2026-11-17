package com.fandom.auchatstudio

import android.content.ContentUris
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import android.provider.MediaStore
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
import com.fandom.auchatstudio.core.MessageType
import com.fandom.auchatstudio.core.ThemePreset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * Raw screens for the Google Play listing. The cast and stories are invented for the listing
 * (no real artist names or photos); avatars are emoji drawn on a gradient.
 *
 * It wipes the app's data first, so it only runs when asked for with the instrumentation
 * argument `storeShots=true` and is skipped in the normal device-test matrix.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class StoreScreenshotTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val app get() = instrumentation.targetContext.applicationContext as AuChatApp

    private val outputDir: File by lazy {
        val fromAgp = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
        val dir = if (fromAgp != null) File(fromAgp) else File(instrumentation.targetContext.externalMediaDirs.first(), "store")
        dir.apply { mkdirs() }
    }

    @Before
    fun stage() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("storeShots") == "true")
        shell("am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS")
        // A tidy status bar: fixed time, full battery and signal, no notification icons.
        shell("settings put global sysui_demo_allowed 1")
        shell("am broadcast -a com.android.systemui.demo -e command enter")
        shell("am broadcast -a com.android.systemui.demo -e command clock -e hhmm 2109")
        shell("am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false")
        shell("am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 -e fully true")
        shell("am broadcast -a com.android.systemui.demo -e command network -e mobile show -e level 4 -e datatype none")
        shell("am broadcast -a com.android.systemui.demo -e command notifications -e visible false")
        runBlocking { seedListingData() }
    }

    @After
    fun tidy() {
        shell("am broadcast -a com.android.systemui.demo -e command exit")
        shell("cmd uimode night no")
    }

    private fun shell(command: String) {
        instrumentation.uiAutomation.executeShellCommand(command).close()
        Thread.sleep(300)
    }

    private fun shot(name: String) {
        rule.waitForIdle()
        Thread.sleep(1_800)
        val bitmap: Bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
        FileOutputStream(File(outputDir, "store_$name.png")).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** Emoji on a two-colour gradient, imported the same way a picked photo would be. */
    private suspend fun avatar(emoji: String, from: Int, to: Int, name: String): String? {
        val size = 384
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val bg = Paint().apply { shader = LinearGradient(0f, 0f, size.toFloat(), size.toFloat(), from, to, Shader.TileMode.CLAMP) }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bg)
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = size * 0.55f; textAlign = Paint.Align.CENTER }
        val y = size / 2f - (text.descent() + text.ascent()) / 2f
        canvas.drawText(emoji, size / 2f, y, text)
        val file = File(instrumentation.targetContext.cacheDir, "$name.png")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return app.container.repository.importImage(Uri.fromFile(file), "avatar_store")
    }

    private suspend fun seedListingData() {
        val repo = app.container.repository
        repo.ready.first { it }
        repo.stories.first().forEach { repo.deleteStory(it.story.id) }
        repo.characters.first().forEach { repo.deleteCharacter(it) }
        app.container.prefs.dismissGuide()

        val kian = repo.saveCharacter(null, "Kian (Bias)", 0xFFFF6584, false, avatar("🐰", 0xFFFFC1D6.toInt(), 0xFFFF6584.toInt(), "kian"))
        val may = repo.saveCharacter(null, "Mây", 0xFF6C4DFF, false, avatar("🦊", 0xFFFFD8A8.toInt(), 0xFFFF8A3D.toInt(), "may"))
        val rin = repo.saveCharacter(null, "Rin", 0xFF00B894, false, avatar("🐻", 0xFFD7F5E9.toInt(), 0xFF2EC4A0.toInt(), "rin"))
        val me = repo.saveCharacter(null, "Y/N (Bạn)", 0xFF43D8C9, true, avatar("🐱", 0xFFE3DCFF.toInt(), 0xFF8C7BFF.toInt(), "me"))

        // Oldest first: the last one created is the "continue" card on the home screen.
        val group = repo.createStory(GROUP_TITLE, ThemePreset.INSTAGRAM_DM)
        repo.setChatTitle(group, "Hội không ngủ 🌙")
        repo.addMessage(group, null, MessageType.NARRATION, "Rin đã thêm bạn vào nhóm")
        repo.addMessage(group, rin, MessageType.TEXT, "Ai còn thức điểm danh", timeLabel = "23:58")
        repo.addMessage(group, may, MessageType.TEXT, "Có em 🙋‍♀️")
        repo.addMessage(group, kian, MessageType.TEXT, "Mai tập sớm đó nha mấy đứa")
        repo.addMessage(group, me, MessageType.TEXT, "Anh cũng đang thức mà 😆", timeLabel = "23:59")
        repo.addMessage(group, kian, MessageType.VOICE, voiceSeconds = 9)
        repo.addMessage(group, may, MessageType.TEXT, "Ủa anh hát ru tụi em hả 🤭")
        repo.addMessage(group, null, MessageType.NARRATION, "Kian đã rời khỏi cuộc trò chuyện")

        val fansign = repo.createStory(FANSIGN_TITLE, ThemePreset.LYSN_BUBBLE)
        repo.setChatTitle(fansign, "Kian")
        repo.addMessage(fansign, null, MessageType.NARRATION, "Hôm sau")
        repo.addMessage(fansign, kian, MessageType.TEXT, "Hôm qua em đứng hàng thứ ba đúng không?", timeLabel = "08:12")
        repo.addMessage(fansign, me, MessageType.TEXT, "Sao anh nhớ hay vậy 😳")
        repo.addMessage(fansign, kian, MessageType.TEXT, "Cái băng rôn màu tím khó quên lắm")
        repo.addMessage(fansign, kian, MessageType.TEXT, "Lần sau nhớ giơ cao hơn nha 💜", timeLabel = "08:13")
        repo.addMessage(fansign, me, MessageType.TEXT, "Dạ 🥹🥹")
        repo.addMessage(fansign, null, MessageType.NARRATION, "Đã xem")

        val night = repo.createStory(NIGHT_TITLE, ThemePreset.WEVERSE_DM)
        repo.setChatTitle(night, "Kian")
        repo.addMessage(night, null, MessageType.NARRATION, "02:07 · Kian đã gửi cho bạn một tin nhắn")
        repo.addMessage(night, kian, MessageType.TEXT, "Ngủ chưa đó?", timeLabel = "02:07")
        repo.addMessage(night, me, MessageType.TEXT, "Chưa… sao anh còn thức vậy", timeLabel = "02:08")
        repo.addMessage(night, kian, MessageType.TEXT, "Vừa tập xong. Tự nhiên muốn nhắn cho em")
        repo.addMessage(night, kian, MessageType.VOICE, voiceSeconds = 14, timeLabel = "02:09")
        repo.addMessage(night, me, MessageType.TEXT, "Anh vừa hát bài mới hả?? 😳")
        repo.addMessage(night, kian, MessageType.TEXT, "Bí mật. Em là người nghe đầu tiên đó")
        repo.addMessage(night, null, MessageType.NARRATION, "Đang nhập…")
    }

    private fun waitForHome() = rule.waitUntilAtLeastOneExists(hasTestTag("home_list"), 10_000)

    private fun openStory(title: String, firstLine: String) {
        waitForHome()
        rule.onNodeWithTag("home_list").performScrollToNode(hasText(title))
        rule.onNodeWithText(title).performClick()
        rule.waitUntilAtLeastOneExists(hasText(firstLine), 10_000)
    }

    private fun backToHome() {
        Espresso.pressBack()
        waitForHome()
    }

    @Test
    fun listingScreens() {
        waitForHome()
        rule.waitUntilAtLeastOneExists(hasText(NIGHT_TITLE), 10_000)
        shot("01_home")

        openStory(NIGHT_TITLE, "Ngủ chưa đó?")
        shot("02_studio_weverse")

        rule.onNodeWithTag("play_story").performClick()
        rule.waitUntilAtLeastOneExists(hasTestTag("player_surface"), 5_000)
        repeat(9) {
            rule.onNodeWithTag("player_surface").performClick()
            rule.waitForIdle()
        }
        shot("03_player")
        Espresso.pressBack()
        rule.waitUntilAtLeastOneExists(hasTestTag("composer_input"), 5_000)

        rule.onNodeWithTag("footer_export").performClick()
        rule.waitUntilAtLeastOneExists(hasText("Chia sẻ"), 20_000)
        saveLatestExport("04_export")
        backToHome()

        openStory(FANSIGN_TITLE, "Sao anh nhớ hay vậy 😳")
        shot("05_studio_lysn")
        backToHome()

        openStory(GROUP_TITLE, "Ai còn thức điểm danh")
        shot("06_studio_instagram")
        backToHome()

        rule.onNodeWithContentDescription("Nhân vật").performClick()
        rule.waitUntilAtLeastOneExists(hasTestTag("fab_add_character"), 5_000)
        shot("07_characters")
        Espresso.pressBack()

        shell("cmd uimode night yes")
        Thread.sleep(1_500)
        waitForHome()
        shot("08_home_dark")
    }

    /** Copies the newest image the app saved to the gallery next to the screenshots. */
    private fun saveLatestExport(name: String) {
        val resolver = instrumentation.targetContext.contentResolver
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        resolver.query(
            collection,
            arrayOf(MediaStore.Images.Media._ID),
            null,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC",
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return
            val uri = ContentUris.withAppendedId(collection, cursor.getLong(0))
            val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return
            FileOutputStream(File(outputDir, "store_$name.png")).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private companion object {
        const val NIGHT_TITLE = "Tin nhắn lúc 2 giờ sáng"
        const val FANSIGN_TITLE = "Sau buổi fansign"
        const val GROUP_TITLE = "Nhóm chat lúc nửa đêm"
    }
}
