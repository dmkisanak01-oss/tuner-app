package com.example.tuner.domain

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.room.withTransaction
import com.example.tuner.data.db.GameEntryEntity
import com.example.tuner.data.db.TunerDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GameRepository(private val context: Context, private val db: TunerDatabase) {

    fun observeRows() = db.gameEntryDao().observeRows()

    /** Heuristik: kategori GAME atau flag lama FLAG_IS_GAME. Game yang tidak terdeteksi ditambah manual (S12, M3). */
    suspend fun scan() = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val now = System.currentTimeMillis()
        val games = pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
            .filter { it.packageName != context.packageName && isGame(it) }
            .map { it.packageName to pm.getApplicationLabel(it).toString() }
        db.withTransaction {
            val dao = db.gameEntryDao()
            dao.insertIgnore(
                games.map { (pkg, label) ->
                    GameEntryEntity(packageName = pkg, label = label, firstSeenAt = now, createdAt = now, updatedAt = now)
                },
            )
            games.forEach { (pkg, label) -> dao.refresh(pkg, label, now) }
            dao.markMissing(games.map { it.first }, now)
        }
    }

    @Suppress("DEPRECATION")
    private fun isGame(info: ApplicationInfo): Boolean =
        info.category == ApplicationInfo.CATEGORY_GAME || (info.flags and ApplicationInfo.FLAG_IS_GAME) != 0
}
