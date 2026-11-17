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

/** A ready-made short script so a new user gets a finished-looking story in one tap. */
data class StoryTemplate(
    val id: String,
    val emoji: String,
    val title: String,
    val hook: String,
    val theme: ThemePreset,
    val lines: List<TemplateLine>,
)

/**
 * Built-in quick-start scripts. All content is original and fictional; names come from the
 * user's own characters, never from the app.
 */
object StoryTemplates {
    private fun idol(text: String, time: String? = null) = TemplateLine(TemplateRole.IDOL, text, timeLabel = time)
    private fun friend(text: String) = TemplateLine(TemplateRole.FRIEND, text)
    private fun me(text: String, time: String? = null) = TemplateLine(TemplateRole.USER, text, timeLabel = time)
    private fun narrator(text: String) = TemplateLine(TemplateRole.NARRATOR, text)
    private fun voice(seconds: Int, time: String? = null) =
        TemplateLine(TemplateRole.IDOL, type = MessageType.VOICE, voiceSeconds = seconds, timeLabel = time)

    val all: List<StoryTemplate> = listOf(
        StoryTemplate(
            id = "good_night",
            emoji = "🌙",
            title = "Chúc ngủ ngon",
            hook = "Tin nhắn thoại lúc nửa đêm",
            theme = ThemePreset.LYSN_BUBBLE,
            lines = listOf(
                narrator("23:47"),
                idol("Ngủ chưa đó?", "23:47"),
                me("Chưa ạ, em đang nghe nhạc của anh"),
                idol("Vậy nghe thêm một bài nữa rồi ngủ nha"),
                voice(9, "23:50"),
                me("Giọng anh ấm quá 🥺"),
                idol("Ngủ ngon. Mơ đẹp nhé", "23:52"),
            ),
        ),
        StoryTemplate(
            id = "after_concert",
            emoji = "🎤",
            title = "Sau concert",
            hook = "Anh ấy thấy banner của bạn",
            theme = ThemePreset.WEVERSE_DM,
            lines = listOf(
                narrator("Sau đêm diễn"),
                idol("Hôm nay mọi người hát to lắm, anh nghe rõ hết"),
                me("Em ở khu B, vẫy lightstick mỏi cả tay luôn!!"),
                idol("Thấy rồi nha. Cái banner màu tím đúng không?"),
                me("ANH THẤY THẬT Á 😭"),
                idol("Về cẩn thận, nhớ ăn tối đó"),
                narrator("Đã xem"),
            ),
        ),
        StoryTemplate(
            id = "secret_dm",
            emoji = "🤫",
            title = "Tin nhắn bí mật",
            hook = "Acc phụ chỉ nhắn cho một người",
            theme = ThemePreset.INSTAGRAM_DM,
            lines = listOf(
                idol("Đừng nói ai là anh nhắn cho em nha", "01:08"),
                me("Em hứa. Nhưng sao anh lại dùng acc này?"),
                idol("Acc phụ. Chỉ để nói chuyện với một người thôi"),
                me("…một người là ai ạ"),
                idol("Đoán xem 🙂", "01:12"),
                narrator("Đã xem"),
            ),
        ),
        StoryTemplate(
            id = "group_chat",
            emoji = "🍪",
            title = "Nhóm chat giờ nghỉ",
            hook = "Ai đã ăn hết hộp bánh?",
            theme = ThemePreset.LYSN_BUBBLE,
            lines = listOf(
                friend("Ai lấy mất hộp bánh trong phòng tập rồi??"),
                idol("Không phải anh"),
                friend("Trên áo anh còn vụn bánh kìa"),
                me("Em làm chứng 😂"),
                idol("…mai anh mua đền hai hộp"),
                narrator("Mọi người đã thả tim"),
            ),
        ),
    )

    fun byId(id: String): StoryTemplate? = all.firstOrNull { it.id == id }
}

/** One-tap narrator lines offered in the composer. */
object NarrationPresets {
    val all: List<String> = listOf("Đã xem", "Hôm sau", "3 ngày sau", "Đang nhập…", "Cuộc gọi nhỡ")
}
