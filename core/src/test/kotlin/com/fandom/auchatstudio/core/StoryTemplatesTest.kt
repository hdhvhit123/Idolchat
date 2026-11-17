package com.fandom.auchatstudio.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StoryTemplatesTest {
    @Test
    fun `template ids are unique and resolvable`() {
        val ids = StoryTemplates.all.map { it.id }
        assertThat(ids).containsNoDuplicates()
        ids.forEach { assertThat(StoryTemplates.byId(it)).isNotNull() }
        assertThat(StoryTemplates.byId("missing")).isNull()
    }

    @Test
    fun `every template is a playable script`() {
        assertThat(StoryTemplates.all).isNotEmpty()
        StoryTemplates.all.forEach { template ->
            assertThat(template.lines.size).isAtLeast(4)
            // Both sides of the conversation appear, otherwise the chat looks one-sided.
            assertThat(template.lines.map { it.role }).containsAtLeast(TemplateRole.IDOL, TemplateRole.USER)
        }
    }

    @Test
    fun `lines are well formed for their type`() {
        StoryTemplates.all.flatMap { it.lines }.forEach { line ->
            when (line.type) {
                MessageType.TEXT -> assertThat(line.role).isNotEqualTo(TemplateRole.NARRATOR)
                MessageType.NARRATION -> assertThat(line.role).isEqualTo(TemplateRole.NARRATOR)
                MessageType.VOICE ->
                    assertThat(line.voiceSeconds).isIn(VoiceFormat.MIN_SECONDS..VoiceFormat.MAX_SECONDS)
                MessageType.IMAGE -> throw AssertionError("Templates cannot ship images")
            }
            if (line.role == TemplateRole.NARRATOR) assertThat(line.type).isEqualTo(MessageType.NARRATION)
            line.timeLabel?.let { assertThat(TimeLabel.normalize(it)).isEqualTo(it) }
        }
    }

    @Test
    fun `localized fills the words in script order and leaves voice notes alone`() {
        val template = StoryTemplates.byId("good_night")!!
        val words = (1..template.textSlots).map { "line $it" }
        val filled = template.localized("Good night", "A voice note at midnight", words)

        assertThat(filled.title).isEqualTo("Good night")
        assertThat(filled.hook).isEqualTo("A voice note at midnight")
        assertThat(filled.lines.filter { it.type != MessageType.VOICE }.map { it.text }).isEqualTo(words)
        val voice = filled.lines.single { it.type == MessageType.VOICE }
        assertThat(voice.text).isEmpty()
        assertThat(voice.voiceSeconds).isEqualTo(9)
        // The shape of the script is untouched.
        assertThat(filled.lines.map { it.role }).isEqualTo(template.lines.map { it.role })
        assertThat(filled.lines.map { it.timeLabel }).isEqualTo(template.lines.map { it.timeLabel })
    }

    @Test(expected = IllegalArgumentException::class)
    fun `localized refuses a translation with the wrong number of lines`() {
        val template = StoryTemplates.byId("secret_dm")!!
        template.localized("t", "h", List(template.textSlots - 1) { "x" })
    }
}
