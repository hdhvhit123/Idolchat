package com.fandom.auchatstudio.i18n

import android.content.Context
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.fandom.auchatstudio.R
import com.fandom.auchatstudio.core.StoryTemplate
import com.fandom.auchatstudio.core.StoryTemplates
import com.fandom.auchatstudio.core.ThemePreset
import com.fandom.auchatstudio.data.RepositoryStrings

/** The translated words of one built-in template. */
private data class TemplateWords(@StringRes val title: Int, @StringRes val hook: Int, @ArrayRes val lines: Int)

private val templateWords = mapOf(
    "good_night" to TemplateWords(R.string.template_good_night_title, R.string.template_good_night_hook, R.array.template_good_night_lines),
    "after_concert" to TemplateWords(R.string.template_after_concert_title, R.string.template_after_concert_hook, R.array.template_after_concert_lines),
    "secret_dm" to TemplateWords(R.string.template_secret_dm_title, R.string.template_secret_dm_hook, R.array.template_secret_dm_lines),
    "group_chat" to TemplateWords(R.string.template_group_chat_title, R.string.template_group_chat_hook, R.array.template_group_chat_lines),
)

/** The built-in templates with their words in the current language. */
fun Context.storyTemplates(): List<StoryTemplate> = StoryTemplates.all.map { template ->
    val words = templateWords.getValue(template.id)
    template.localized(
        title = getString(words.title),
        hook = getString(words.hook),
        texts = resources.getStringArray(words.lines).toList(),
    )
}

/** [storyTemplates], read again whenever the language changes. */
@Composable
fun rememberStoryTemplates(): List<StoryTemplate> {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    return remember(configuration) { context.storyTemplates() }
}

/** The one-line description of a chat skin. */
@StringRes
fun ThemePreset.descriptionRes(): Int = when (this) {
    ThemePreset.LYSN_BUBBLE -> R.string.theme_lysn_description
    ThemePreset.WEVERSE_DM -> R.string.theme_weverse_description
    ThemePreset.INSTAGRAM_DM -> R.string.theme_instagram_description
}

/**
 * [RepositoryStrings] from the app's resources. Every read goes to [context] again, so a
 * language change made while the app is running is picked up by the next story written.
 */
class ResourceRepositoryStrings(private val context: Context) : RepositoryStrings {
    override val untitledStory: String get() = context.getString(R.string.untitled_story)
    override fun copyTitle(title: String): String = context.getString(R.string.copy_title, title)
    override val defaultUserName: String get() = context.getString(R.string.default_user_name)
    override val defaultIdolName: String get() = context.getString(R.string.default_idol_name)
    override val defaultMemberName: String get() = context.getString(R.string.default_member_name)
    override val sampleTitle: String get() = context.getString(R.string.sample_title)
    override val sampleLines: List<String> get() = context.resources.getStringArray(R.array.sample_lines).toList()
}
