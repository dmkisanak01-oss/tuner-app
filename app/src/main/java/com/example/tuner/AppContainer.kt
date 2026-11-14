package com.example.tuner

import android.content.Context
import androidx.room.Room
import com.example.tuner.data.db.TunerDatabase
import com.example.tuner.data.prefs.SettingsStore
import com.example.tuner.domain.GameRepository
import com.example.tuner.domain.TuningService
import com.example.tuner.system.ExecutorRouter
import com.example.tuner.system.PrivilegedExecutor
import com.example.tuner.system.RootExecutor
import com.example.tuner.system.ShizukuExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** DI manual untuk M1 (Hilt belum dipakai: mengurangi konfigurasi build yang belum bisa diuji). */
class AppContainer(context: Context) {
    private val app = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val db: TunerDatabase = Room.databaseBuilder(app, TunerDatabase::class.java, "tuner.db").build()
    val settings = SettingsStore(app)
    val shizuku = ShizukuExecutor(app).also { it.start() }
    val root = RootExecutor()
    val executor: PrivilegedExecutor = ExecutorRouter(settings, shizuku, root, scope)
    val games = GameRepository(app, db)
    val tuning = TuningService(executor, db)
}
