package com.fandom.auchatstudio.data

/**
 * The few words the repository writes into the user's data itself: names for placeholder
 * characters, the title of a copy, and the sample story seeded on first launch. They are taken
 * in the device's language at the moment they are written and are ordinary user data from then
 * on, so changing the language later never rewrites a story.
 */
interface RepositoryStrings {
    /** Title for a story the user did not name. */
    val untitledStory: String

    /** Title of a copy of [title]. */
    fun copyTitle(title: String): String

    /** Placeholder names for characters a template needs but the cast does not have yet. */
    val defaultUserName: String
    val defaultIdolName: String
    val defaultMemberName: String

    /** Sample story seeded on first launch. */
    val sampleTitle: String

    /** The sample's eight written lines, in order (see [SampleData.messages]). */
    val sampleLines: List<String>
}

/** English words, used where no Android resources are at hand (JVM tests). */
object PlainRepositoryStrings : RepositoryStrings {
    override val untitledStory = "Untitled story"
    override fun copyTitle(title: String) = "$title (copy)"
    override val defaultUserName = "Y/N (You)"
    override val defaultIdolName = "Idol"
    override val defaultMemberName = "Member"
    override val sampleTitle = "Midnight messages"
    override val sampleLines = listOf(
        "02:13 · Minho sent you a message",
        "Still awake?",
        "How did you know I wasn't asleep 👀",
        "Just a guess. A good one, huh",
        "Were you singing?? Is that a new song 😳",
        "It's a secret. Don't tell anyone",
        "What are you two whispering about 😏",
        "Hyunjin has seen it",
    )
}
