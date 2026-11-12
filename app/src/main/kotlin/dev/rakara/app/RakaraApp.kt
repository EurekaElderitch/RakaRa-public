package dev.rakara.app

import android.app.Application
import android.content.Context
import dev.rakara.app.data.EngineRepository
import dev.rakara.app.data.LibraryRepository
import dev.rakara.app.data.ReadMarkRepository
import dev.rakara.app.data.NotificationRepository
import dev.rakara.engine.EngineManager
import dev.rakara.engine.EngineUpdater


class RakaraApp : Application() {

    val engines: EngineManager by lazy {
        EngineManager(this).also { it.cleanupTombstones() }
    }

    val engineUpdater: EngineUpdater by lazy {
        EngineUpdater(
            context = this,
            installer = engines.installer,
            indexUrl = getString(R.string.engine_repo_url),
        )
    }

    val library: LibraryRepository by lazy { LibraryRepository(this) }
    val readMarks: ReadMarkRepository by lazy { ReadMarkRepository(this) }
    val notifications: NotificationRepository by lazy { NotificationRepository(this) }
    val engineRepo: EngineRepository by lazy { EngineRepository(engines) }

    override fun onCreate() {
        super.onCreate()
        
        engines
    }

    companion object {
        fun from(context: Context): RakaraApp =
            context.applicationContext as RakaraApp
    }
}