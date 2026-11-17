package com.fandom.auchatstudio.core

/** Who says a template line. Roles are mapped onto the user's own cast when a story is created. */
enum class TemplateRole { IDOL, FRIEND, USER, NARRATOR }

data class TemplateLine(
    val role: TemplateRole,
    val text: String = "",
    val type: MessageType = if (role == TemplateRole.NARRATOR) MessageType.NARRATION else MessageType.TEXT,
    val voiceSeconds: Int = 0,
    val timeLabel: String? = null,
)

/**
 * A ready-made short script so a new user gets a finished-looking story in one tap.
 *
 * The script's shape (who speaks, voice notes, times) is shared by every language; the words
 * are not. [StoryTemplates.all] therefore holds templates without words, and the app fills them
 * in from its translated resources with [localized].
 */
data class StoryTemplate(
    val id: String,
    val emoji: String,
    val theme: ThemePreset,
    val lines: List<TemplateLine>,
    val title: String = "",
    val hook: String = "",
) {
    /** How many lines carry words: every line except voice notes, in script order. */
    val textSlots: Int get() = lines.count { it.type != MessageType.VOICE }

    /** This script with its words. [texts] has one entry per [textSlots], in order. */
    fun localized(title: String, hook: String, texts: List<String>): StoryTemplate {
        require(texts.size == textSlots) { "Template $id needs $textSlots lines of text, got ${texts.size}" }
        val words = texts.iterator()
        return copy(
            title = title,
            hook = hook,
            lines = lines.map { if (it.type == MessageType.VOICE) it else it.copy(text = words.next()) },
        )
    }
}

/**
 * Built-in quick-start scripts. All content is original and fictional; names come from the
 * user's own characters, never from the app. The comments give the Vietnamese original of
 * each line so the shape can be read without opening the resources.
 */
object StoryTemplates {
    private fun idol(time: String? = null) = TemplateLine(TemplateRole.IDOL, timeLabel = time)
    private fun friend() = TemplateLine(TemplateRole.FRIEND)
    private fun me(time: String? = null) = TemplateLine(TemplateRole.USER, timeLabel = time)
    private fun narrator() = TemplateLine(TemplateRole.NARRATOR)
    private fun voice(seconds: Int, time: String? = null) =
        TemplateLine(TemplateRole.IDOL, type = MessageType.VOICE, voiceSeconds = seconds, timeLabel = time)

    val all: List<StoryTemplate> = listOf(
        StoryTemplate(
            id = "good_night",
            emoji = "🌙",
            theme = ThemePreset.LYSN_BUBBLE,
            lines = listOf(
                narrator(), // 23:47
                idol("23:47"), // Ngủ chưa đó?
                me(), // Chưa ạ, em đang nghe nhạc của anh
                idol(), // Vậy nghe thêm một bài nữa rồi ngủ nha
                voice(9, "23:50"),
                me(), // Giọng anh ấm quá 🥺
                idol("23:52"), // Ngủ ngon. Mơ đẹp nhé
            ),
        ),
        StoryTemplate(
            id = "after_concert",
            emoji = "🎤",
            theme = ThemePreset.WEVERSE_DM,
            lines = listOf(
                narrator(), // Sau đêm diễn
                idol(), // Hôm nay mọi người hát to lắm, anh nghe rõ hết
                me(), // Em ở khu B, vẫy lightstick mỏi cả tay luôn!!
                idol(), // Thấy rồi nha. Cái banner màu tím đúng không?
                me(), // ANH THẤY THẬT Á 😭
                idol(), // Về cẩn thận, nhớ ăn tối đó
                narrator(), // Đã xem
            ),
        ),
        StoryTemplate(
            id = "secret_dm",
            emoji = "🤫",
            theme = ThemePreset.INSTAGRAM_DM,
            lines = listOf(
                idol("01:08"), // Đừng nói ai là anh nhắn cho em nha
                me(), // Em hứa. Nhưng sao anh lại dùng acc này?
                idol(), // Acc phụ. Chỉ để nói chuyện với một người thôi
                me(), // …một người là ai ạ
                idol("01:12"), // Đoán xem 🙂
                narrator(), // Đã xem
            ),
        ),
        StoryTemplate(
            id = "group_chat",
            emoji = "🍪",
            theme = ThemePreset.LYSN_BUBBLE,
            lines = listOf(
                friend(), // Ai lấy mất hộp bánh trong phòng tập rồi??
                idol(), // Không phải anh
                friend(), // Trên áo anh còn vụn bánh kìa
                me(), // Em làm chứng 😂
                idol(), // …mai anh mua đền hai hộp
                narrator(), // Mọi người đã thả tim
            ),
        ),
    )

    fun byId(id: String): StoryTemplate? = all.firstOrNull { it.id == id }
}
