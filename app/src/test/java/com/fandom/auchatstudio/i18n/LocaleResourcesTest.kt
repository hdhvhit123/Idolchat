package com.fandom.auchatstudio.i18n

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fandom.auchatstudio.R
import com.fandom.auchatstudio.core.StoryTemplates
import com.fandom.auchatstudio.data.PlainRepositoryStrings
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Keeps the translations in step with each other. The app follows the phone's language, so a
 * language whose file lost a line, a list item or a format argument would show the wrong text
 * or crash only on phones set to that language.
 */
@RunWith(AndroidJUnit4::class)
class LocaleResourcesTest {
    private val res = File("src/main/res")

    /** One translation file, as written on disk. */
    private class Translation(
        val strings: Map<String, String>,
        val plurals: Map<String, Map<String, String>>,
        val arrays: Map<String, List<String>>,
    )

    private fun Element.children(tag: String): List<Element> {
        val nodes = getElementsByTagName(tag)
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }

    private fun read(folder: File): Translation {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(folder, "strings.xml"))
        val root = doc.documentElement
        return Translation(
            strings = root.children("string")
                .filter { it.getAttribute("translatable") != "false" }
                .associate { it.getAttribute("name") to it.textContent },
            plurals = root.children("plurals").associate { p ->
                p.getAttribute("name") to p.children("item").associate { it.getAttribute("quantity") to it.textContent }
            },
            arrays = root.children("string-array").associate { a ->
                a.getAttribute("name") to a.children("item").map { it.textContent }
            },
        )
    }

    /** The text Android shows for a value as written in the file. */
    private fun unescape(text: String): String =
        Regex("""\\(.)""").replace(text) { if (it.groupValues[1] == "n") "\n" else it.groupValues[1] }

    /** Resource folder qualifier ("vi", "b+zh+Hans") to the tag the system uses ("vi", "zh-Hans"). */
    private fun tagOf(qualifier: String) = qualifier.removePrefix("b+").replace('+', '-')

    private val translated: Map<String, File>
        get() = res.listFiles()!!
            .filter { it.name.startsWith("values-") && File(it, "strings.xml").exists() }
            .associateBy { it.name.removePrefix("values-") }

    private fun formatArgs(text: String): List<String> =
        Regex("""%(\d+\$)?[sd]""").findAll(text).map { it.value }.sorted().toList()

    @Test
    fun everyTranslatedFolderIsOfferedInTheLanguageSettingAndBack() {
        val config = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File(res, "xml/locales_config.xml")).documentElement
        val offered = config.children("locale").map { it.getAttribute("android:name") }
        // English lives in the default folder.
        assertThat(offered).containsExactlyElementsIn(translated.keys.map(::tagOf) + "en")
    }

    @Test
    fun everyLanguageHasTheSameTextsListsAndArguments() {
        val base = read(File(res, "values"))
        assertThat(translated).isNotEmpty()
        translated.forEach { (qualifier, folder) ->
            val t = read(folder)
            assertWithMessage("strings in $qualifier").that(t.strings.keys).containsExactlyElementsIn(base.strings.keys)
            assertWithMessage("plurals in $qualifier").that(t.plurals.keys).containsExactlyElementsIn(base.plurals.keys)
            assertWithMessage("lists in $qualifier").that(t.arrays.keys).containsExactlyElementsIn(base.arrays.keys)
            base.strings.forEach { (name, text) ->
                assertWithMessage("$name in $qualifier").that(t.strings.getValue(name)).isNotEmpty()
                assertWithMessage("arguments of $name in $qualifier")
                    .that(formatArgs(t.strings.getValue(name))).isEqualTo(formatArgs(text))
            }
            base.plurals.forEach { (name, forms) ->
                val mine = t.plurals.getValue(name)
                assertWithMessage("$name in $qualifier has an 'other' form").that(mine).containsKey("other")
                mine.forEach { (quantity, text) ->
                    assertWithMessage("arguments of $name/$quantity in $qualifier")
                        .that(formatArgs(text)).isEqualTo(formatArgs(forms.getValue("other")))
                }
            }
            base.arrays.forEach { (name, items) ->
                assertWithMessage("length of $name in $qualifier").that(t.arrays.getValue(name)).hasSize(items.size)
            }
        }
    }

    @Test
    fun eachLanguageIsPickedForItsPhonesAndTemplatesFitTheirScripts() {
        val qualifiers = listOf("en") + translated.keys
        qualifiers.forEach { qualifier ->
            RuntimeEnvironment.setQualifiers(qualifier)
            val context = ApplicationProvider.getApplicationContext<Context>()
            val expectedTagline = if (qualifier == "en") {
                read(File(res, "values")).strings.getValue("app_tagline")
            } else {
                read(translated.getValue(qualifier)).strings.getValue("app_tagline")
            }
            assertWithMessage("tagline on a $qualifier phone").that(context.getString(R.string.app_tagline))
                .isEqualTo(unescape(expectedTagline))

            // localized() refuses a translation with the wrong number of lines, so this also
            // checks every template's words against its script.
            val templates = context.storyTemplates()
            assertThat(templates.map { it.id }).isEqualTo(StoryTemplates.all.map { it.id })
            templates.forEach { assertWithMessage("${it.id} in $qualifier").that(it.title).isNotEmpty() }

            val strings = ResourceRepositoryStrings(context)
            assertWithMessage("sample in $qualifier").that(strings.sampleLines).hasSize(PlainRepositoryStrings.sampleLines.size)
            assertThat(strings.copyTitle("X")).contains("X")
        }
    }

    @Test
    fun englishResourcesMatchTheWordsUsedWithoutResources() {
        RuntimeEnvironment.setQualifiers("en")
        val strings = ResourceRepositoryStrings(ApplicationProvider.getApplicationContext())
        val plain = PlainRepositoryStrings
        assertThat(strings.untitledStory).isEqualTo(plain.untitledStory)
        assertThat(strings.copyTitle("A")).isEqualTo(plain.copyTitle("A"))
        assertThat(strings.defaultUserName).isEqualTo(plain.defaultUserName)
        assertThat(strings.defaultIdolName).isEqualTo(plain.defaultIdolName)
        assertThat(strings.defaultMemberName).isEqualTo(plain.defaultMemberName)
        assertThat(strings.sampleTitle).isEqualTo(plain.sampleTitle)
        assertThat(strings.sampleLines).isEqualTo(plain.sampleLines)
    }
}
