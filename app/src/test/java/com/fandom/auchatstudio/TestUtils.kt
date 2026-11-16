package com.fandom.auchatstudio

import android.net.Uri
import com.fandom.auchatstudio.data.AppPrefs
import com.fandom.auchatstudio.data.ImageStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Records deletes instead of touching the file system. */
class FakeImageStore : ImageStore {
    val deleted = mutableListOf<String>()
    var nextImport: String? = "/fake/files/images/picked.webp"

    override suspend fun importImage(source: Uri, prefix: String, maxSize: Int): String? = nextImport

    override suspend fun delete(path: String?) {
        if (path != null) deleted += path
    }

    /** Sources passed to [copy], in order. */
    val copied = mutableListOf<String>()
    var failCopy = false

    override suspend fun copy(path: String, prefix: String): String? =
        if (failCopy) null else "$path.copy${copied.size}".also { copied += path }

    /** What [listOlderThan] reports as being on disk. */
    var stored: List<String> = emptyList()

    override suspend fun listOlderThan(olderThanMillis: Long): List<String> = stored
}

class FakeAppPrefs(override var sampleSeeded: Boolean = false, guide: Boolean = true) : AppPrefs {
    private val _showGuide = MutableStateFlow(guide)
    override val showGuide: StateFlow<Boolean> = _showGuide

    override fun dismissGuide() {
        _showGuide.value = false
    }
}

/** Polls [condition], giving background threads (Room) real time to deliver results. */
fun waitFor(timeoutMs: Long = 5_000, step: () -> Unit = {}, condition: () -> Boolean) {
    val end = System.currentTimeMillis() + timeoutMs
    while (true) {
        step()
        if (condition()) return
        if (System.currentTimeMillis() > end) throw AssertionError("Condition not met within $timeoutMs ms")
        Thread.sleep(10)
    }
}
