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
            assertThat(template.title).isNotEmpty()
            assertThat(template.title.length).isAtMost(NameRules.MAX_TITLE_LENGTH)
            assertThat(template.lines.size).isAtLeast(4)
            // Both sides of the conversation appear, otherwise the chat looks one-sided.
            assertThat(template.lines.map { it.role }).containsAtLeast(TemplateRole.IDOL, TemplateRole.USER)
        }
    }

    @Test
    fun `lines are well formed for their type`() {
        StoryTemplates.all.flatMap { it.lines }.forEach { line ->
            when (line.type) {
                MessageType.TEXT -> assertThat(line.text).isNotEmpty()
                MessageType.NARRATION -> {
                    assertThat(line.role).isEqualTo(TemplateRole.NARRATOR)
                    assertThat(line.text).isNotEmpty()
                }
                MessageType.VOICE ->
                    assertThat(line.voiceSeconds).isIn(VoiceFormat.MIN_SECONDS..VoiceFormat.MAX_SECONDS)
                MessageType.IMAGE -> throw AssertionError("Templates cannot ship images")
            }
            if (line.role == TemplateRole.NARRATOR) assertThat(line.type).isEqualTo(MessageType.NARRATION)
            line.timeLabel?.let { assertThat(TimeLabel.normalize(it)).isEqualTo(it) }
        }
    }

    @Test
    fun `narration presets are short and distinct`() {
        assertThat(NarrationPresets.all).containsNoDuplicates()
        NarrationPresets.all.forEach { assertThat(it.length).isAtMost(20) }
    }
}
