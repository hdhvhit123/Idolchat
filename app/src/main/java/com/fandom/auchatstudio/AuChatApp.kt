package com.fandom.auchatstudio

import android.app.Application
import com.fandom.auchatstudio.data.AppDatabase
import com.fandom.auchatstudio.data.FileStore
import com.fandom.auchatstudio.data.StudioRepository
import com.fandom.auchatstudio.export.ChatExporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Manual dependency container: the app is small enough that a DI framework isn't worth it. */
class AppContainer(app: Application) {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(app) }
    val repository: StudioRepository by lazy { StudioRepository(database, FileStore(app)) }
    val exporter: ChatExporter by lazy { ChatExporter(app) }
}

class AuChatApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        appScope.launch { container.repository.seedIfEmpty() }
    }
}
