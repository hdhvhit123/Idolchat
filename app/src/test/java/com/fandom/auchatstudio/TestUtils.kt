package com.fandom.auchatstudio

import android.net.Uri
import com.fandom.auchatstudio.data.ImageStore

/** Records deletes instead of touching the file system. */
class FakeImageStore : ImageStore {
    val deleted = mutableListOf<String>()
    var nextImport: String? = "/fake/files/images/picked.webp"

    override suspend fun importImage(source: Uri, prefix: String, maxSize: Int): String? = nextImport

    override suspend fun delete(path: String?) {
        if (path != null) deleted += path
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
